package br.com.reservasala.api.services;

import br.com.reservasala.api.models.Evento;
import br.com.reservasala.api.models.Reserva;
import br.com.reservasala.api.models.ZoomAccount;
import br.com.reservasala.api.models.ZoomTokenResponse;
import jakarta.transaction.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import java.net.http.HttpRequest;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ZoomService {

      private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    private static final Logger log = LoggerFactory.getLogger(ZoomService.class);

    @Value("${zoom.api.account-id}")
    private String zoomAccountId;

    @Value("${zoom.api.client-id}")
    private String zoomClientId;

    @Value("${zoom.api.client-secret}")
    private String zoomClientSecret;

    /**
     * E-mail padrão da TI para co-host (pode ser sobrescrito via properties)
     */
    @Value("${zoom.default.cohost.email:suporte.ti@grupoccaa.com}")
    private String zoomDefaultCoHostEmail;

    /**
     * Janela de folga para reservas presenciais transformadas em reuniões (minutos)
     */
    @Value("${zoom.meeting.buffer-minutes:15}")
    private int bufferMinutes;

    private final RestTemplate restTemplate;

    public ZoomService() {
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(8000);
        rf.setReadTimeout(15000);
        this.restTemplate = new RestTemplate(rf);
    }

    // ===================== Token cache =====================

    private volatile String cachedToken;
    private volatile long tokenExpiresAtEpochSeconds; // epoch seconds

    private synchronized String getAccessToken() {
        long now = System.currentTimeMillis() / 1000;
        // Renova 30s antes de expirar
        if (cachedToken != null && tokenExpiresAtEpochSeconds - 30 > now) {
            return cachedToken;
        }

        String credentials = Base64.getEncoder()
                .encodeToString((zoomClientId + ":" + zoomClientSecret).getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + credentials);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<String> request = new HttpEntity<>("", headers);
        String url = "https://zoom.us/oauth/token?grant_type=account_credentials&account_id=" + zoomAccountId;

        try {
            ResponseEntity<ZoomTokenResponse> response = restTemplate.postForEntity(url, request, ZoomTokenResponse.class);
            ZoomTokenResponse body = response.getBody();
            if (body == null || body.getAccessToken() == null) {
                log.error("Resposta de token inválida do Zoom: {}", response.getStatusCode());
                return null;
            }
            cachedToken = body.getAccessToken();
            long expiresIn = Math.max(60, body.getExpiresIn());
            tokenExpiresAtEpochSeconds = now + expiresIn;
            return cachedToken;
        } catch (HttpClientErrorException.TooManyRequests e) {
            String retry = e.getResponseHeaders() != null ? e.getResponseHeaders().getFirst("Retry-After") : null;
            log.warn("Rate limited ao obter token do Zoom. Retry-After={}", retry);
            return null;
        } catch (Exception e) {
            log.error("Erro ao obter token do Zoom", e);
            return null;
        }
    }

    // ===================== Helpers =====================

    private static String toZoomStartTime(LocalDateTime ldt) {
        // Envie sempre com timezone (UTC/Z) em ISO 8601
        return ldt.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private static Long asLong(JsonNode node) {
        if (node == null) return null;
        if (node.isNumber()) return node.asLong();
        try { return Long.parseLong(node.asText()); } catch (NumberFormatException e) { return null; }
    }

    private static HttpHeaders authJson(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private static String shortText(String s) {
        if (s == null) return "";
        String oneLine = s.replace('\n', ' ').replace('\r', ' ');
        return oneLine.length() <= 500 ? oneLine : oneLine.substring(0, 500) + "...";
    }

    private Map<String, Object> parseCreateResponse(String body) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(body);
        Map<String, Object> details = new HashMap<>();
        Long id = asLong(json.get("id"));
        if (id != null) details.put("id", id);
        if (json.has("join_url")) details.put("url", json.get("join_url").asText());
        if (json.has("registration_url")) details.put("registrationUrl", json.get("registration_url").asText());
        if (json.has("start_url")) details.put("startUrl", json.get("start_url").asText());
        if (json.has("password")) details.put("passcode", json.get("password").asText());
        if (json.has("uuid")) details.put("uuid", json.get("uuid").asText());
        return details;
    }

    private String fetchMeetingRegistrationUrl(Long meetingId, String occurrenceId, String token) {
        if (meetingId == null || token == null) return null;
        String url = "https://api.zoom.us/v2/meetings/" + meetingId;
        if (occurrenceId != null && !occurrenceId.isBlank()) {
            url += "?occurrence_id=" + URLEncoder.encode(occurrenceId, StandardCharsets.UTF_8);
        }
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(authJson(token)), String.class);
            JsonNode json = mapper.readTree(response.getBody());
            String registrationUrl = json.path("registration_url").asText(null);
            String approvalType = json.path("settings").path("approval_type").asText("n/a");
            log.info("Zoom GET meeting {} -> approval_type={}, has_registration_url={}",
                    meetingId, approvalType, registrationUrl != null && !registrationUrl.isBlank());
            if (registrationUrl == null || registrationUrl.isBlank()) return null;
            return registrationUrl;
        } catch (Exception e) {
            log.warn("Não foi possível obter registration_url para meeting {}: {}", meetingId, e.getMessage());
            return null;
        }
    }

    private void ensureMeetingRegistrationEnabled(Long meetingId, boolean recurringFixedTime, String token) {
        if (meetingId == null || token == null) return;
        Map<String, Object> settings = new HashMap<>();
        settings.put("approval_type", 0); // automatic approval with registration required
        if (recurringFixedTime) {
            settings.put("registration_type", 1); // one registration for all occurrences
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("settings", settings);
        String url = "https://api.zoom.us/v2/meetings/" + meetingId;
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.PATCH, new HttpEntity<>(payload, authJson(token)), String.class);
            log.info("Zoom PATCH meeting {} to enable registration -> status={}", meetingId, response.getStatusCode());
        } catch (Exception e) {
            log.warn("Não foi possível forçar inscrição na reunião {}: {}", meetingId, e.getMessage());
        }
    }
        
       

    // ===================== API Pública =====================

    /**
     * Cria reunião para um Evento, usando a conta Zoom informada.
     * Retorna mapa com chaves: id (Long), url (join_url), startUrl (do host), passcode (password)
     */
    public Map<String, Object> createMeeting(Evento evento, ZoomAccount hostAccount) {
        String token = getAccessToken();
        if (token == null) {
            log.error("Não foi possível criar reunião: token de acesso nulo");
            return null;
        }

        long durationInMinutes = ChronoUnit.MINUTES.between(evento.getStartTime(), evento.getEndTime());
        if (durationInMinutes <= 0) durationInMinutes = 30; // fallback seguro

        // settings
        Map<String, Object> settings = new HashMap<>();
        settings.put("waiting_room", true);
        settings.put("join_before_host", false);
        settings.put("mute_upon_entry", true);
        settings.put("host_video", false);
        settings.put("participant_video", false);    // Sem anfitriões alternativos: organizador recebe e-mail/confirmação, mas não é setado como alternative_host.
        settings.put("approval_type", 2); // Inscrição gerenciada pela nossa plataforma; Zoom não exige registro próprio


        // payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("topic", Optional.ofNullable(evento.getTitle()).orElse("Reunião"));
        payload.put("type", 2); // scheduled
        payload.put("start_time", toZoomStartTimeUTCFromEvento(evento.getStartTime()));
        payload.put("timezone", "UTC");
        payload.put("duration", (int) durationInMinutes);
        payload.put("agenda", Optional.ofNullable(evento.getDescription()).orElse(""));
        payload.put("settings", settings);
        log.info("Zoom payload single eventoId={} startTimeDb={} endTimeDb={} start_time={} timezone={} jvmZone={}",
                evento.getId(),
                evento.getStartTime(),
                evento.getEndTime(),
                payload.get("start_time"),
                payload.get("timezone"),
                java.time.ZoneId.systemDefault());

        HttpHeaders headers = authJson(token);
        String url = "https://api.zoom.us/v2/users/" + hostAccount.getAccountMail() + "/meetings";

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(payload, headers), String.class);
            Map<String, Object> details = parseCreateResponse(response.getBody());
            log.info("Zoom create single meeting (eventoId={}, registrationRequired={}) -> status={}, meetingId={}, hasJoinUrl={}, hasRegistrationUrl={}, raw={}",
                    evento.getId(),
                    evento.isRegistrationRequired(),
                    response.getStatusCode(),
                    details.get("id"),
                    details.get("url") != null,
                    details.get("registrationUrl") != null,
                    shortText(response.getBody()));
            return details;
        } catch (HttpClientErrorException.TooManyRequests e) {
            String retry = e.getResponseHeaders() != null ? e.getResponseHeaders().getFirst("Retry-After") : null;
            log.warn("Rate limited ao criar reunião (Evento {}). Retry-After={}", evento.getId(), retry);
            return null;
        } catch (Exception e) {
            log.error("Erro ao criar reunião no Zoom (Evento {})", evento.getId(), e);
            return null;
        }
    }

    /**
     * Cria reunião para uma Reserva (sala física + link), aplicando buffer de tempo.
     */
    public Map<String, Object> createMeetingForReserva(Reserva reserva, ZoomAccount hostAccount) {
        String token = getAccessToken();
        if (token == null) {
            log.error("Não foi possível criar reunião para a reserva: token nulo.");
            return null;
        }

        LocalDateTime start = reserva.getStartTime().minusMinutes(bufferMinutes);
        LocalDateTime end   = reserva.getEndTime().plusMinutes(bufferMinutes);
        long duration = Math.max(1, ChronoUnit.MINUTES.between(start, end));

        Map<String, Object> settings = new HashMap<>();
        settings.put("waiting_room", true);
        settings.put("join_before_host", false);
        settings.put("mute_upon_entry", true);    // Sem anfitriões alternativos: organizador recebe e-mail/confirmação, mas não é setado como alternative_host.
        

        String agenda = "Reunião presencial na sala: " + reserva.getSala().getNome()
                + ". Link online para participantes remotos.\n\nDescrição: "
                + Optional.ofNullable(reserva.getDescription()).orElse("");

        Map<String, Object> payload = new HashMap<>();
        payload.put("topic", Optional.ofNullable(reserva.getTitle()).orElse("Reserva de Sala"));
        payload.put("type", 2);
        payload.put("start_time", toZoomStartTimeUTCFromReservaLocal(start));
        payload.put("timezone", "UTC");
        payload.put("duration", (int) duration);
        payload.put("agenda", agenda);
        payload.put("settings", settings);
        log.info("Zoom payload reservaId={} startLocalWithBuffer={} endLocalWithBuffer={} start_time={} timezone={} jvmZone={}",
                reserva.getId(),
                start,
                end,
                payload.get("start_time"),
                payload.get("timezone"),
                java.time.ZoneId.systemDefault());

        HttpHeaders headers = authJson(token);
        String url = "https://api.zoom.us/v2/users/" + hostAccount.getAccountMail() + "/meetings";

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(payload, headers), String.class);
            Map<String, Object> details = parseCreateResponse(response.getBody());
            // Informação interna útil para automação (NÃO envie a participantes!)
            if (hostAccount.getHostkey() != null) {
                details.put("hostKey", hostAccount.getHostkey());
            }
            return details;
        } catch (Exception e) {
            log.error("Erro ao criar reunião no Zoom para a reserva {}", reserva.getId(), e);
            return null;
        }
    }

    /**
     * Deleta reunião por ID no Zoom. Idempotente: 404 é tratado como sucesso.
     */
    public void deleteMeeting(Long meetingId) throws Exception {
        String token = getAccessToken();
        if (token == null) throw new Exception("Token de acesso nulo ao deletar meeting");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        String url = "https://api.zoom.us/v2/meetings/" + meetingId;
        try {
            restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(headers), String.class);
            log.info("Reunião deletada no Zoom: {}", meetingId);
        } catch (HttpClientErrorException.NotFound e) {
            log.info("Reunião {} já inexistente (404) no Zoom. Tratando como deletada.", meetingId);
        } catch (Exception e) {
            log.warn("Erro ao deletar reunião {} no Zoom", meetingId, e);
            throw e;
        }
    }


  public Map<String, Object> createRecurringMeeting(Evento parent, List<Evento> occurrences, ZoomAccount hostAccount) {
    String token = getAccessToken();
    if (token == null) {
        log.error("Não foi possível criar reunião recorrente: token nulo");
        return null;
    }
    if (occurrences == null || occurrences.isEmpty()) {
        log.error("Ocorrências vazias para criação de reunião recorrente (Evento {})", parent.getId());
        return null;
    }

    // A série completa é: parent + children
    List<Evento> allOccurrences = new ArrayList<>();
    allOccurrences.add(parent);
    allOccurrences.addAll(occurrences);

    // Ordena ocorrências
    allOccurrences.sort(Comparator.comparing(Evento::getStartTime));

    // Primeira data e duração (minutos) da reunião
   LocalDateTime first = allOccurrences.stream()
        .map(Evento::getStartTime)
        .min(LocalDateTime::compareTo)
        .orElse(parent.getStartTime());

if (parent.getStartTime().isBefore(first)) {
    first = parent.getStartTime();
}
    LocalDateTime end0   = occurrences.get(0).getEndTime();
    long duration = Math.max(1, ChronoUnit.MINUTES.between(parent.getStartTime(), parent.getEndTime()));

    // Calcula repeat_interval em semanas (default=1). Se o intervalo entre "semanas" variar, fixa 1.
    int repeatIntervalWeeks = 1;
    if (occurrences.size() >= 3) {
        // tenta inferir o passo SEMANAL olhando a diferença entre a 1ª e a 3ª ocorrência
        long days = ChronoUnit.DAYS.between(occurrences.get(0).getStartTime(),
                                            occurrences.get(2).getStartTime());
        if (days % 7 == 0 && days >= 14) {
            repeatIntervalWeeks = (int) (days / 7 / 2); // ex.: seg/qua em semanas alternadas → 2
            repeatIntervalWeeks = Math.max(1, repeatIntervalWeeks);
        }
    }

    // weekly_days do Zoom: 1=Dom,2=Seg,3=Ter,4=Qua,5=Qui,6=Sex,7=Sáb
    // a partir das ocorrências, coletamos todos os dias distintos
    String weeklyDaysCsv = mapJavaDowsToZoomCsv(
            allOccurrences.stream().map(Evento::getStartTime).toList()
    );

    // Recurrence payload
    Map<String, Object> recurrence = new HashMap<>();
    recurrence.put("type", 2); // Weekly
    recurrence.put("repeat_interval", repeatIntervalWeeks);
    recurrence.put("weekly_days", weeklyDaysCsv);

    // Encerramento por contagem (N ocorrências)
    // OBS: Zoom também aceita end_date_time, mas aqui usamos end_times para refletir exatamente as "occurrences" geradas no sistema
    // end_times precisa considerar a série completa (parent + children)
    recurrence.put("end_times", allOccurrences.size());

    // Settings básicos (ajuste conforme sua política)
    Map<String, Object> settings = new HashMap<>();
    settings.put("waiting_room", true);
    settings.put("join_before_host", false);
    settings.put("mute_upon_entry", true);
    settings.put("approval_type", 2); // Inscrição gerenciada pela nossa plataforma; Zoom não exige registro próprio

    // Corpo da criação
    Map<String, Object> payload = new HashMap<>();
    payload.put("topic", Optional.ofNullable(parent.getTitle()).orElse("Reunião recorrente"));
    payload.put("type", 8); // Recurring with fixed time
    payload.put("start_time", toZoomStartTimeUTCFromEvento(first)); // ISO UTC
    payload.put("duration", (int) duration);
    payload.put("timezone", "UTC"); // importante para o Zoom interpretar o start_time
    payload.put("agenda", Optional.ofNullable(parent.getDescription()).orElse(""));
    payload.put("recurrence", recurrence);
    payload.put("settings", settings);
    log.info("Zoom payload recurring parentEventoId={} firstDb={} start_time={} timezone={} jvmZone={}",
            parent.getId(),
            first,
            payload.get("start_time"),
            payload.get("timezone"),
            java.time.ZoneId.systemDefault());

    HttpHeaders headers = authJson(token);
    String url = "https://api.zoom.us/v2/users/" + hostAccount.getAccountMail() + "/meetings";

    try {
        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(payload, headers), String.class);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(response.getBody());

        Map<String, Object> out = new HashMap<>();
        Long id = asLong(json.get("id"));
        if (id != null) out.put("id", id);
        if (json.has("join_url"))  out.put("url", json.get("join_url").asText());
        if (json.has("registration_url")) out.put("registrationUrl", json.get("registration_url").asText());
        if (json.has("start_url")) out.put("startUrl", json.get("start_url").asText());
        if (json.has("password"))  out.put("passcode", json.get("password").asText());
        if (json.has("uuid")) out.put("uuid", json.get("uuid").asText());
        log.info("Zoom create recurring meeting (parentEventoId={}, registrationRequired={}) -> status={}, meetingId={}, hasJoinUrl={}, hasRegistrationUrl={}, raw={}",
                parent.getId(),
                parent.isRegistrationRequired(),
                response.getStatusCode(),
                out.get("id"),
                out.get("url") != null,
                out.get("registrationUrl") != null,
                shortText(response.getBody()));

        // occurrenceMap: start_time(UTC ISO) -> occurrence_id (útil para cancelar/editar ocorrência específica)
        Map<String, String> mapStartToOccId = new HashMap<>();
        if (json.has("occurrences") && json.get("occurrences").isArray()) {
            for (JsonNode occ : json.get("occurrences")) {
                String occId    = occ.path("occurrence_id").asText(null);
                String occStart = occ.path("start_time").asText(null); // ISO 8601 UTC
                if (occId != null && occStart != null) {
                    // normaliza retirando segundos/nanos para bater com nossa geração
                    String key = normalizeIsoUtc(occStart);
                    mapStartToOccId.put(key, occId);
                }
            }
        }
        out.put("occurrenceMap", mapStartToOccId);
        if (parent.isRegistrationRequired() && !out.containsKey("registrationUrl")) {
            Long meetingId = (Long) out.get("id");
            ensureMeetingRegistrationEnabled(meetingId, true, token);
            String registrationUrl = fetchMeetingRegistrationUrl(meetingId, null, token);
            if (registrationUrl != null && !registrationUrl.isBlank()) {
                out.put("registrationUrl", registrationUrl);
            }
        }
        return out;

    } catch (Exception e) {
        log.error("Erro ao criar reunião recorrente no Zoom (Evento {})", parent.getId(), e);
        return null;
    }
}


    /** Deleta UMA ocorrência específica dentro de uma reunião recorrente. */
    public void deleteMeetingOccurrence(Long meetingId, String occurrenceId) throws Exception {
        String token = getAccessToken();
        if (token == null) throw new Exception("Token nulo ao deletar ocorrência");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        String url = "https://api.zoom.us/v2/meetings/" + meetingId + "?occurrence_id=" + occurrenceId;
        try {
            restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(headers), String.class);
            log.info("Ocorrência {} deletada da reunião {} no Zoom", occurrenceId, meetingId);
        } catch (HttpClientErrorException.NotFound e) {
            log.info("Ocorrência {} ou reunião {} já inexistente no Zoom (404)", occurrenceId, meetingId);
        } catch (Exception e) {
            log.warn("Erro ao deletar ocorrência {} da reunião {}", occurrenceId, meetingId, e);
            throw e;
        }
    }

   
  public String getFreshStartUrl(Long meetingId, String occurrenceId) throws Exception {
        String base = "https://api.zoom.us/v2/meetings/" + meetingId;
        if (occurrenceId != null && !occurrenceId.isBlank()) {
            base += "?occurrence_id=" + URLEncoder.encode(occurrenceId, StandardCharsets.UTF_8);
        }
        HttpRequest req = HttpRequest.newBuilder(URI.create(base))
                .header("Authorization", "Bearer " + getAccessToken())
                .GET()
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() / 100 != 2) {
            throw new RuntimeException("Zoom GET /meetings falhou: " + resp.statusCode() + " - " + resp.body());
        }
        JsonNode json = mapper.readTree(resp.body());
        String fresh = json.path("start_url").asText(null);
        if (fresh == null || fresh.isBlank()) {
            throw new RuntimeException("start_url não retornado pelo Zoom");
        }
        return fresh;
    }



/** Evento salvo em UTC no banco (LocalDateTime "naive" em UTC) -> ISO UTC para Zoom. */
private String toZoomStartTimeUTCFromEvento(LocalDateTime ldt) {
    var odt = ldt.withSecond(0).withNano(0).atOffset(ZoneOffset.UTC);
    return odt.format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);
}

/** Reserva salva em horário local -> converte para UTC para Zoom. */
private String toZoomStartTimeUTCFromReservaLocal(LocalDateTime ldt) {
    var zone = java.time.ZoneId.systemDefault();
    var zdt  = ldt.atZone(zone).withSecond(0).withNano(0).withZoneSameInstant(java.time.ZoneOffset.UTC);
    return zdt.format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);
}

/** Converte lista de datas (java: 1=Seg..7=Dom) em CSV no padrão Zoom (1=Dom..7=Sáb) */
private String mapJavaDowsToZoomCsv(List<LocalDateTime> starts) {
    Set<Integer> set = new TreeSet<>();
    for (LocalDateTime dt : starts) {
        int javaDow = dt.getDayOfWeek().getValue(); // 1..7 (Seg..Dom)
        int zoomDow = (javaDow == 7) ? 1 : (javaDow + 1); // map: Dom->1, Seg->2, ...
        set.add(zoomDow);
    }
    return set.stream().map(String::valueOf).collect(Collectors.joining(","));
}

/** Normaliza ISO vindo do Zoom (UTC) para comparar com chaves geradas localmente (sem segundos/nanos) */
private String normalizeIsoUtc(String iso) {
    try {
        var odt = java.time.OffsetDateTime.parse(iso);
        odt = odt.withSecond(0).withNano(0);
        return odt.format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    } catch (Exception e) {
        return iso;
    }
}



    // ===================== Relatórios de reuniões (past_meetings) =====================

    public record PastParticipant(String name, String email, LocalDateTime joinTime, LocalDateTime leaveTime, Integer durationMinutes) {}
    public record PastMeetingSummary(String meetingUuid, String topic, LocalDateTime startTime, LocalDateTime endTime,
                                     Integer durationMinutes, Integer participantsCount, Integer peakParticipants,
                                     Integer totalMinutes, String recordingUrl, String recordingStatus) {}
    public record PastMeetingReport(PastMeetingSummary summary, List<PastParticipant> participants) {}
    public record MeetingRegistrant(String id, String name, String email, String status, String joinUrl, LocalDateTime registeredAt) {}

    public List<MeetingRegistrant> fetchMeetingRegistrants(Long meetingId, String occurrenceId, String status) {
        String token = getAccessToken();
        if (token == null || meetingId == null) return List.of();

        HttpHeaders headers = authJson(token);
        List<MeetingRegistrant> out = new ArrayList<>();
        String next = null;
        String finalStatus = (status == null || status.isBlank()) ? "approved" : status.trim().toLowerCase();

        do {
            StringBuilder url = new StringBuilder("https://api.zoom.us/v2/meetings/")
                    .append(meetingId)
                    .append("/registrants?page_size=300")
                    .append("&status=").append(URLEncoder.encode(finalStatus, StandardCharsets.UTF_8));
            if (occurrenceId != null && !occurrenceId.isBlank()) {
                url.append("&occurrence_id=").append(URLEncoder.encode(occurrenceId, StandardCharsets.UTF_8));
            }
            if (next != null && !next.isBlank()) {
                url.append("&next_page_token=").append(URLEncoder.encode(next, StandardCharsets.UTF_8));
            }

            try {
                ResponseEntity<String> response = restTemplate.exchange(
                        url.toString(), HttpMethod.GET, new HttpEntity<>(headers), String.class);
                JsonNode json = mapper.readTree(response.getBody());
                if (json.has("registrants") && json.get("registrants").isArray()) {
                    for (JsonNode r : json.get("registrants")) {
                        String first = r.path("first_name").asText("");
                        String last = r.path("last_name").asText("");
                        String fullName = (first + " " + last).trim();
                        String name = fullName.isBlank() ? r.path("name").asText("-") : fullName;
                        out.add(new MeetingRegistrant(
                                r.path("id").asText(null),
                                name,
                                r.path("email").asText("-"),
                                r.path("status").asText(finalStatus),
                                r.path("join_url").asText(""),
                                parseZoomDateTime(r.path("create_time").asText(null))
                        ));
                    }
                }
                next = json.path("next_page_token").asText(null);
                if (next != null && next.isBlank()) next = null;
            } catch (HttpClientErrorException.NotFound e) {
                return List.of();
            } catch (Exception e) {
                log.error("Erro ao buscar inscritos da reunião {} no Zoom", meetingId, e);
                return List.of();
            }
        } while (next != null);

        return out;
    }

    /**
     * Para reuniões recorrentes, o Zoom atribui um UUID distinto a cada ocorrência que
     * realmente aconteceu. Este método lista as instâncias passadas do meeting e retorna
     * o UUID da ocorrência cujo horário de início está mais próximo de {@code approxStart}
     * (dentro de uma tolerância de 60 minutos), ou {@code null} se não houver correspondência.
     */
    public String findPastInstanceUuid(Long meetingId, LocalDateTime approxStart) {
        if (meetingId == null || approxStart == null) return null;
        String token = getAccessToken();
        if (token == null) return null;

        HttpHeaders headers = authJson(token);
        String url = "https://api.zoom.us/v2/past_meetings/" + meetingId + "/instances";
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            JsonNode json = mapper.readTree(response.getBody());
            JsonNode meetings = json.path("meetings");
            if (!meetings.isArray()) return null;

            String bestUuid = null;
            long bestDiffSeconds = Long.MAX_VALUE;
            for (JsonNode m : meetings) {
                String uuid = m.path("uuid").asText(null);
                LocalDateTime start = parseZoomDateTime(m.path("start_time").asText(null));
                if (uuid == null || start == null) continue;
                long diff = Math.abs(java.time.Duration.between(approxStart, start).toSeconds());
                if (diff < bestDiffSeconds) {
                    bestDiffSeconds = diff;
                    bestUuid = uuid;
                }
            }
            // Aceita correspondência dentro de 60 minutos (3600 s)
            return (bestDiffSeconds <= 3600) ? bestUuid : null;
        } catch (Exception e) {
            log.warn("Erro ao listar instâncias past_meetings para meeting {}: {}", meetingId, e.getMessage());
            return null;
        }
    }

    public PastMeetingReport fetchPastMeetingReport(String meetingUuidOrId) {
        String token = getAccessToken();
        if (token == null) {
            log.error("Não foi possível obter token do Zoom para gerar relatório");
            return null;
        }

        String encoded = encodeMeetingKey(meetingUuidOrId);
        PastMeetingSummary summary = fetchPastMeetingSummary(token, encoded, meetingUuidOrId);
        if (summary == null) return null;
        List<PastParticipant> participants = fetchPastMeetingParticipants(token, encoded);
        return new PastMeetingReport(summary, participants);
    }

    private PastMeetingSummary fetchPastMeetingSummary(String token, String encodedId, String original) {
        HttpHeaders headers = authJson(token);
        String url = "https://api.zoom.us/v2/past_meetings/" + encodedId;
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            JsonNode json = mapper.readTree(response.getBody());
            String uuid = json.path("uuid").asText(original);
            String topic = json.path("topic").asText(null);
            LocalDateTime start = parseZoomDateTime(json.path("start_time").asText(null));
            LocalDateTime end = parseZoomDateTime(json.path("end_time").asText(null));
            Integer duration = json.hasNonNull("duration") ? json.get("duration").asInt() : null;
            Integer participants = json.hasNonNull("participants_count") ? json.get("participants_count").asInt() : null;
            // Zoom retorna "participants_count" como total de entradas únicas; não há campo separado
            // de pico na resposta /past_meetings — usamos o mesmo valor até disponibilidade de outro endpoint.
            Integer totalMinutes = json.hasNonNull("total_minutes") ? json.get("total_minutes").asInt() : null;

            RecordingInfo rec = fetchRecordingInfo(token, encodedId);
            return new PastMeetingSummary(
                    uuid, topic, start, end, duration, participants, /* peakParticipants */ participants, totalMinutes,
                    rec.url(), rec.status()
            );
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Past meeting não encontrado no Zoom: {}", original);
            return null;
        } catch (Exception e) {
            log.error("Erro ao buscar past meeting {} no Zoom", original, e);
            return null;
        }
    }

    private List<PastParticipant> fetchPastMeetingParticipants(String token, String encodedId) {
        HttpHeaders headers = authJson(token);
        List<PastParticipant> out = new ArrayList<>();
        String next = null;
        do {
            String url = "https://api.zoom.us/v2/past_meetings/" + encodedId + "/participants?page_size=300";
            if (next != null && !next.isBlank()) {
                url += "&next_page_token=" + URLEncoder.encode(next, StandardCharsets.UTF_8);
            }
            try {
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
                JsonNode json = mapper.readTree(response.getBody());
                if (json.has("participants") && json.get("participants").isArray()) {
                    for (JsonNode p : json.get("participants")) {
                        String name = p.path("name").asText(null);
                        String email = p.path("user_email").asText(null);
                        LocalDateTime join = parseZoomDateTime(p.path("join_time").asText(null));
                        LocalDateTime leave = parseZoomDateTime(p.path("leave_time").asText(null));
                        Integer duration = p.hasNonNull("duration") ? p.get("duration").asInt() : null;
                        out.add(new PastParticipant(name, email, join, leave, duration));
                    }
                }
                next = json.path("next_page_token").asText(null);
            } catch (HttpClientErrorException.NotFound e) {
                log.warn("Participantes não encontrados para meeting {}", encodedId);
                break;
            } catch (Exception e) {
                log.warn("Erro ao buscar participantes do past meeting {}: {}", encodedId, e.getMessage());
                break;
            }
        } while (next != null && !next.isBlank());
        return out;
    }

    private record RecordingInfo(String url, String status) {}

    private RecordingInfo fetchRecordingInfo(String token, String encodedId) {
        HttpHeaders headers = authJson(token);
        String url = "https://api.zoom.us/v2/meetings/" + encodedId + "/recordings";
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            JsonNode json = mapper.readTree(response.getBody());
            String status = json.path("status").asText(null);
            String recordingUrl = null;
            if (json.has("recording_files") && json.get("recording_files").isArray()) {
                for (JsonNode file : json.get("recording_files")) {
                    if (file.has("play_url")) {
                        recordingUrl = file.get("play_url").asText();
                        break;
                    }
                }
                if (recordingUrl == null) {
                    for (JsonNode file : json.get("recording_files")) {
                        if (file.has("download_url")) {
                            recordingUrl = file.get("download_url").asText();
                            break;
                        }
                    }
                }
            }
            return new RecordingInfo(recordingUrl, status);
        } catch (HttpClientErrorException.NotFound e) {
            return new RecordingInfo(null, "not_found");
        } catch (Exception e) {
            log.warn("Erro ao buscar gravação do meeting {}: {}", encodedId, e.getMessage());
            return new RecordingInfo(null, "error");
        }
    }

    private LocalDateTime parseZoomDateTime(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try {
            var odt = java.time.OffsetDateTime.parse(iso);
            return odt.toLocalDateTime();
        } catch (Exception e) {
            return null;
        }
    }

    private String encodeMeetingKey(String meetingUuidOrId) {
        try {
            // IDs numéricos não precisam de encoding; UUIDs do Zoom que contêm '/' ou '//'
            // devem ser double URL-encoded conforme documentação da API do Zoom.
            if (meetingUuidOrId.chars().allMatch(Character::isDigit)) {
                return meetingUuidOrId;
            }
            String once = URLEncoder.encode(meetingUuidOrId, StandardCharsets.UTF_8);
            return URLEncoder.encode(once, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return meetingUuidOrId;
        }
    }

}
