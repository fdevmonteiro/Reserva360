package br.com.reservasala.api.controllers;

import br.com.reservasala.api.models.Evento;
import br.com.reservasala.api.models.EventStatus;
import br.com.reservasala.api.models.EventoRequestDTO;
import br.com.reservasala.api.models.ZoomAccount;
import br.com.reservasala.api.repository.EventoRepository;
import br.com.reservasala.api.services.EmailService;
import br.com.reservasala.api.services.EventoReportService;
import br.com.reservasala.api.services.GraphApiService;
import br.com.reservasala.api.services.ZoomLicenseManager;
import br.com.reservasala.api.services.ZoomService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/eventos")
public class EventoController {

    private static final Logger logger = LoggerFactory.getLogger(EventoController.class);

    private final EventoRepository eventoRepository;
    private final EmailService emailService;
    private final ZoomService zoomService;
    private final GraphApiService graphApiService;
    private final ZoomLicenseManager zoomLicenseManager;
    private final EventoReportService eventoReportService;
    private final Clock clock;

    public EventoController(
            EventoRepository eventoRepository,
            EmailService emailService,
            ZoomService zoomService,
            GraphApiService graphApiService,
            ZoomLicenseManager zoomLicenseManager,
            EventoReportService eventoReportService,
            Clock clock
    ) {
        this.eventoRepository = eventoRepository;
        this.emailService = emailService;
        this.zoomService = zoomService;
        this.graphApiService = graphApiService;
        this.zoomLicenseManager = zoomLicenseManager;
        this.eventoReportService = eventoReportService;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    // =============== Listagens ===============

    // Busca apenas eventos CONFIRMADOS futuros
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Evento> getAllEventos() {
        return eventoRepository.findByStatusAndStartTimeAfterOrderByStartTimeAsc(
                EventStatus.CONFIRMED, LocalDateTime.now(clock)
        );
    }

    @GetMapping(value = "/proximos", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Evento> getUpcomingEventos() {
        return eventoRepository.findTop5ByStatusAndStartTimeAfterOrderByStartTimeAsc(
                EventStatus.CONFIRMED, LocalDateTime.now(clock)
        );
    }

    @GetMapping(value = "/passados", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Evento> getPastEventos() {
        return eventoRepository.findByStatusAndStartTimeBeforeOrderByStartTimeDesc(
                EventStatus.CONFIRMED, LocalDateTime.now(clock)
        );
    }

    // =============== Criação (pré-reserva) ===============

   @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
@PreAuthorize("isAuthenticated()")
@Transactional
    public ResponseEntity<?> createEvento(@RequestBody @Valid EventoRequestDTO req) {
        LocalDateTime startUtc = toUtc(req.getStartTime());
        LocalDateTime endUtc = toUtc(req.getEndTime());
        logger.info("createEvento request title='{}' startRaw={} endRaw={} -> startUtc={} endUtc={}",
                req.getTitle(), req.getStartTime(), req.getEndTime(), startUtc, endUtc);
        if (startUtc != null && endUtc != null && !startUtc.isBefore(endUtc)) {
            return ResponseEntity.badRequest().body("'startTime' deve ser antes de 'endTime'.");
        }

    // Se NÃO for recorrente → fluxo simples (PENDING + email de confirmação)
    if (req.getRecurrence() == null) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = authentication != null ? authentication.getName() : "system";

        // (opcional) você pode adiar a reserva de licença para a confirmação;
        // se quiser manter aqui, deixe esta linha:
        ZoomAccount available = zoomLicenseManager.findAvailableAccount(
                req.getStartTime(), req.getEndTime(), req.getParticipantCount()
        );
        if (available == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Nenhuma licença do Zoom disponível para este horário. Por favor, tente outro período.");
        }

        Evento ev = toEvento(req);
        ev.setStartTime(startUtc);
        ev.setEndTime(endUtc);
        ev.setCreatedByUsername(currentUser);
        ev.setHostZoomAccount(available); // se preferir, remova e reatribua na confirmação
        ev.setStatus(EventStatus.PENDING);
        ev.setConfirmationToken(UUID.randomUUID().toString());

        Evento saved = eventoRepository.save(ev);
        try { emailService.sendSingleEventConfirmationEmail(saved); } catch (Exception ignored) {}

        record CreateEventoResponse(Long id, String title, LocalDateTime startTime, LocalDateTime endTime, EventStatus status) {}
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CreateEventoResponse(saved.getId(), saved.getTitle(), saved.getStartTime(), saved.getEndTime(), saved.getStatus()));
    }

    // Se for RECORRENTE → gera pai + filhos como PENDING (mesmo token)
    var r = req.getRecurrence();
    if (r.getFrequency() == null || !"WEEKLY".equals(r.getFrequency().name())) {
        return ResponseEntity.badRequest().body("Apenas recorrência semanal suportada nesta versão.");
    }

    startUtc = toUtc(req.getStartTime());
    endUtc = toUtc(req.getEndTime());

    List<LocalDateTime> starts = generateWeeklyStarts(startUtc, r);
    if (starts.isEmpty()) {
        return ResponseEntity.badRequest().body("Nenhuma ocorrência gerada para a regra de recorrência.");
    }

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String currentUser = authentication != null ? authentication.getName() : "system";

    // token único para a série
    String seriesToken = UUID.randomUUID().toString();

    // pai = primeira ocorrência
    Evento parent = toEvento(req);
    parent.setStartTime(starts.get(0));
    parent.setEndTime(starts.get(0).plusMinutes(java.time.Duration.between(startUtc, endUtc).toMinutes()));
    parent.setCreatedByUsername(currentUser);
    parent.setStatus(EventStatus.PENDING);
    parent.setConfirmationToken(seriesToken);
    parent = eventoRepository.save(parent);

    // filhos
    var dur = java.time.Duration.between(startUtc, endUtc);
    for (int i = 1; i < starts.size(); i++) {
        LocalDateTime s = starts.get(i);
        Evento child = toEvento(req);
        child.setStartTime(s);
        child.setEndTime(s.plus(dur));
        child.setParentEventId(parent);
        child.setCreatedByUsername(currentUser);
        child.setStatus(EventStatus.PENDING);
        
        eventoRepository.save(child);
    }

    // um único e-mail (do pai) com link de confirmação da SÉRIE
    try { emailService.sendSeriesConfirmationEmail(parent); } catch (Exception ignored) {}

    record SeriesResp(Long parentId, int totalOccurrences, String confirmationToken) {}
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(new SeriesResp(parent.getId(), starts.size(), seriesToken));
}

    // =============== Landings (GET) sem efeitos colaterais ===============

    @GetMapping(value = "/confirmar", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> confirmarLanding(@RequestParam("token") String token) {
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().contentType(MediaType.TEXT_HTML)
                    .body("<h1>Token inválido</h1>");
        }

        String safeToken = HtmlUtils.htmlEscape(token);
        String html = ("""
            <html><body>
              <h1>Confirmar Evento</h1>
              <form method='POST' action='/api/eventos/confirmar'>
                <input type='hidden' name='token' value='%s'/>
                
                <button type='submit'>Confirmar</button>
              </form>
            </body></html>
        """).formatted(safeToken);
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }

    @GetMapping(value = "/cancelar", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> cancelarLanding(@RequestParam("token") String token) {
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().contentType(MediaType.TEXT_HTML)
                    .body("<h1>Token inválido</h1>");
        }
        String safeToken = HtmlUtils.htmlEscape(token);
        String html = ("""
            <html><body>
              <h1>Cancelar Evento</h1>
              <form method='POST' action='/api/eventos/cancelar'>
                <input type='hidden' name='token' value='%s'/>
                <!-- Se CSRF estiver habilitado, inclua aqui o hidden _csrf -->
                <button type='submit'>Confirmar Cancelamento</button>
              </form>
            </body></html>
        """).formatted(safeToken);
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }

    // =============== Ações (POST) idempotentes ===============

    @PostMapping(value = "/confirmar")
    @Transactional
    public ResponseEntity<String> confirmarPost(@RequestParam("token") String token) {
        return eventoRepository.findByConfirmationToken(token)
                .map(evento -> {
                    // Idempotência
                    if (evento.getStatus() == EventStatus.CONFIRMED) {
                        return ResponseEntity.ok("<h1>Evento já confirmado</h1>");
                    }
                    if (evento.getStatus() == EventStatus.CANCELED) {
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body("<h1>Evento cancelado</h1><p>Não é possível confirmar.</p>");
                    }

                    // (Re)valida licença/host no momento da confirmação, evitando reaproveitar conta ocupada
                    ZoomAccount hostAccount = zoomLicenseManager.findAvailableAccount(
                            evento.getStartTime(),
                            evento.getEndTime(),
                            evento.getParticipantCount() == null ? 0 : evento.getParticipantCount(),
                            evento.getHostZoomAccount() // tenta reaproveitar se estiver livre; senão troca
                    );
                    if (hostAccount == null) {
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body("<h1>Sem licença disponível</h1><p>Tente novamente mais tarde.</p>");
                    }
                    evento.setHostZoomAccount(hostAccount);

                    Map<String, Object> zoomDetails;
                    try {
                        zoomDetails = zoomService.createMeeting(evento, hostAccount);
                    } catch (Exception e) {
                        logger.error("Erro ao criar reunião no Zoom para evento {}", evento.getId(), e);
                        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                                .body("<h1>Falha ao criar a reunião</h1><p>Tente novamente.</p>");
                    }
                    if (zoomDetails == null || zoomDetails.get("id") == null) {
                        logger.error("Criação de reunião no Zoom retornou nulo para evento {}", evento.getId());
                        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                                .body("<h1>Falha ao criar a reunião</h1>");
                    }

                    String joinUrl = (String) zoomDetails.get("url");
                    Object idObj = zoomDetails.get("id");
                    if (idObj instanceof Number n) {
                        evento.setZoomMeetingId(n.longValue());
                    } else if (idObj instanceof String s) {
                        try { evento.setZoomMeetingId(Long.parseLong(s)); } catch (NumberFormatException ignored) {}
                    }
                    // Inscrição gerenciada pelo sistema — link sempre é o join_url do Zoom
                    evento.setLink(joinUrl);
                    if (zoomDetails.containsKey("startUrl")) {
                        evento.setStartUrl((String) zoomDetails.get("startUrl")); // NÃO enviar a participantes
                    }
                    if (zoomDetails.containsKey("uuid")) {
                        evento.setZoomMeetingUuid((String) zoomDetails.get("uuid"));
                    }

                    evento.setStatus(EventStatus.CONFIRMED);
                    evento.setConfirmationToken(null);

                    Evento finalEvento = eventoRepository.save(evento);

                    String passcode = (String) zoomDetails.getOrDefault("passcode", "");
                    try { emailService.sendEventDetailsEmail(finalEvento, passcode); } catch (Exception e) { logger.warn("Email falhou para evento {}: {}", finalEvento.getId(), e.getMessage()); }
                    try { graphApiService.createCalendarEventForEvento(finalEvento.getOrganizerEmail(), finalEvento); } catch (Exception e) { logger.warn("Calendar falhou para evento {}: {}", finalEvento.getId(), e.getMessage()); }

                    return ResponseEntity.ok("<h1>Evento Confirmado!</h1><p>Os detalhes foram enviados por e-mail.</p>");
                })
                .orElse(ResponseEntity.badRequest().body("<h1>Token inválido!</h1>"));
    }

    @PostMapping(value = "/cancelar")
    @Transactional
    public ResponseEntity<String> cancelarPost(@RequestParam("token") String token) {
        return eventoRepository.findByConfirmationToken(token)
                .map(evento -> {
                    if (evento.getStatus() == EventStatus.CANCELED) {
                        return ResponseEntity.ok("<h1>Evento já cancelado</h1>");
                    }

                    // Se já estava confirmado e existe meetingId, tentar apagar no Zoom (best-effort)
                    if (evento.getStatus() == EventStatus.CONFIRMED && evento.getZoomMeetingId() != null) {
                        try { zoomService.deleteMeeting(evento.getZoomMeetingId()); }
                        catch (Exception e) { logger.warn("Falha ao deletar reunião {} no Zoom para evento {}: {}", evento.getZoomMeetingId(), evento.getId(), e.getMessage()); }
                    }

                    evento.setStatus(EventStatus.CANCELED);
                    evento.setConfirmationToken(null);
                    eventoRepository.save(evento);

                    return ResponseEntity.ok("<h1>Evento Cancelado</h1><p>O evento foi cancelado com sucesso.</p>");
                })
                .orElse(ResponseEntity.badRequest().body("<h1>Token inválido!</h1>"));
    }

    // =============== Update / Delete (autorização) ===============

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @eventoSecurityService.isOwner(authentication.name, #id)")
    public ResponseEntity<Evento> updateEvento(@PathVariable Long id, @RequestBody @Valid EventoRequestDTO dto) {
        return eventoRepository.findById(id)
                .map(evento -> {
                    evento.setTitle(dto.getTitle());
                    evento.setDescription(dto.getDescription());
                    // convert OffsetDateTime from DTO to LocalDateTime (UTC) before setting
                    java.time.LocalDateTime startUtc = toUtc(dto.getStartTime());
                    java.time.LocalDateTime endUtc = toUtc(dto.getEndTime());
                    evento.setStartTime(startUtc);
                    evento.setEndTime(endUtc);
                    evento.setParticipantCount(dto.getParticipantCount());
                    evento.setOrganizerEmail(dto.getOrganizerEmail());
                    evento.setCoHostEmail(dto.getCoHostEmail());
                    if (dto.getRegistrationRequired() != null) {
                        evento.setRegistrationRequired(dto.getRegistrationRequired());
                    }
                    Evento updated = eventoRepository.save(evento);
                    return ResponseEntity.ok(updated);
                }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @eventoSecurityService.isOwner(authentication.name, #id)")
    public ResponseEntity<Void> deleteEvento(@PathVariable Long id) {
        return eventoRepository.findById(id)
                .map(evento -> {
                    // Best-effort: tenta deletar reunião no Zoom se existir
                    if (evento.getZoomMeetingId() != null) {
                        try { zoomService.deleteMeeting(evento.getZoomMeetingId()); }
                        catch (Exception e) { logger.warn("Falha ao deletar a reunião {} no Zoom (seguindo com remoção local). Evento {}.", evento.getZoomMeetingId(), evento.getId(), e); }
                    }
                    eventoRepository.delete(evento);
                    return ResponseEntity.noContent().<Void>build();
                }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}/serie")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @eventoSecurityService.isOwner(authentication.name, #id)")
    @Transactional
    public ResponseEntity<?> deleteSerieRecorrente(@PathVariable Long id) {
        return eventoRepository.findById(id)
                .map(evento -> {
                    Evento parent = resolveSeriesRoot(evento);
                    if (parent == null || parent.getId() == null) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(Map.of("error", "Não foi possível identificar a série recorrente."));
                    }

                    List<Evento> children = eventoRepository.findByParentEventId(parent);
                    List<Evento> toDelete = new ArrayList<>();
                    toDelete.add(parent);
                    if (children != null && !children.isEmpty()) {
                        toDelete.addAll(children);
                    }

                    Set<Long> deletedEventoIds = new HashSet<>();
                    Set<Long> meetingIds = new HashSet<>();
                    for (Evento e : toDelete) {
                        if (e == null || e.getId() == null) continue;
                        if (!deletedEventoIds.add(e.getId())) continue;
                        if (e.getZoomMeetingId() != null) {
                            meetingIds.add(e.getZoomMeetingId());
                        }
                    }

                    List<Long> failedMeetingIds = new ArrayList<>();
                    for (Long meetingId : meetingIds) {
                        try {
                            zoomService.deleteMeeting(meetingId);
                        } catch (Exception ex) {
                            logger.warn("Falha ao deletar reunião {} no Zoom para série raiz {}.", meetingId, parent.getId(), ex);
                            failedMeetingIds.add(meetingId);
                        }
                    }

                    if (!failedMeetingIds.isEmpty()) {
                        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                                .body(Map.of(
                                        "error", "Falha ao deletar uma ou mais reuniões no Zoom. Nenhum evento foi removido localmente.",
                                        "meetingIds", failedMeetingIds
                                ));
                    }

                    if (children != null && !children.isEmpty()) {
                        eventoRepository.deleteAll(children);
                    }
                    eventoRepository.delete(parent);

                    return ResponseEntity.noContent().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/confirmar-recorrente", produces = MediaType.TEXT_HTML_VALUE)
public ResponseEntity<String> confirmarRecorrenteLanding(@RequestParam("token") String token) {
    if (token == null || token.isBlank()) {
        return ResponseEntity.badRequest().contentType(MediaType.TEXT_HTML)
                .body("<h1>Token inválido</h1>");
    }
    String safeToken = org.springframework.web.util.HtmlUtils.htmlEscape(token);
    String html = ("""
        <html><body>
          <h1>Confirmar Eventos Recorrentes</h1>
          <form method='POST' action='/api/eventos/confirmar-recorrente'>
            <input type='hidden' name='token' value='%s'/>
            <button type='submit'>Confirmar Evento recorrente</button>
          </form>
        </body></html>
    """).formatted(safeToken);
    return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
}

    
    @PostMapping(value = "/confirmar-recorrente")
    @Transactional
    public ResponseEntity<String> confirmarRecorrentePost(@RequestParam("token") String token) {
        return eventoRepository.findByConfirmationToken(token)
                .map(parent -> {
                    // Verificações básicas
                    if (parent.getStatus() == EventStatus.CONFIRMED) {
                        return ResponseEntity.ok("<h1>Série já confirmada</h1>");
                    }
                    // Carrega ocorrências (inclui o próprio parent se sua modelagem tratar assim)
                    List<Evento> children = eventoRepository.findByParentEventId(parent);
                    if (children == null || children.isEmpty()) {
                        return ResponseEntity.badRequest().body("<h1>Sem ocorrências para esta série</h1>");
                    }

                    // Garantir conta host
                    ZoomAccount host = parent.getHostZoomAccount();
                    if (host == null) {
                        host = zoomLicenseManager.findAvailableAccount(
                                parent.getStartTime(), parent.getEndTime(),
                                parent.getParticipantCount() == null ? 0 : parent.getParticipantCount()
                        );
                        if (host == null) {
                            return ResponseEntity.status(HttpStatus.CONFLICT)
                                    .body("<h1>Sem licença disponível</h1>");
                        }
                        parent.setHostZoomAccount(host);
                    }

                    // Cria série no Zoom
                    Map<String, Object> result = zoomService.createRecurringMeeting(parent, children, host);
                    if (result == null || result.get("id") == null) {
                        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                                .body("<h1>Falha ao criar série no Zoom</h1>");
                    }
                    Long meetingId = (Long) result.get("id");
                    String joinUrl = (String) result.get("url");
                    String passcode = (String) result.getOrDefault("passcode", "");
                    String meetingUuid = (String) result.get("uuid");

                    // Mapa start_time(ISO)->occurrence_id retornado pelo Zoom
                    @SuppressWarnings("unchecked")
                    Map<String, String> zoomMap = (Map<String, String>) result.get("occurrenceMap");

                    List<Evento> all = new java.util.ArrayList<>();
                    all.add(parent);
                    all.addAll(children);
                    // Atribui meetingId/joinUrl para todas, e tenta casar occurrenceId por horário
                   for (Evento e : all) {
                    // Monta a chave no mesmo formato que o Zoom retornou (ISO UTC, sem segundos/nanos)
                    String key = e.getStartTime()
                        .withSecond(0).withNano(0)
                        .atOffset(ZoneOffset.UTC)
                        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

                    // Busca no mapa retornado pelo Zoom
                    String occId = zoomMap.get(key);

                    e.setZoomMeetingId(meetingId);
                    e.setLink(joinUrl);
                    if (meetingUuid != null && !meetingUuid.isBlank()) {
                        e.setZoomMeetingUuid(meetingUuid);
                    }
                    e.setZoomOccurrenceId(occId); // <- salva qual occurrence no Zoom corresponde a esse filho
                    e.setStatus(EventStatus.CONFIRMED);
                    e.setConfirmationToken(null);

                    eventoRepository.save(e);
                }
                    // E-mail + calendário
                    try { emailService.sendEventDetailsEmail(parent, passcode); } catch (Exception ignored) {}
                    try { graphApiService.createCalendarEventForEvento(parent.getOrganizerEmail(), parent); } catch (Exception ignored) {}

                    return ResponseEntity.ok("<h1>Série confirmada!</h1>");
                })
                .orElse(ResponseEntity.badRequest().body("<h1>Token inválido!</h1>"));
    }

    private Evento toEvento(EventoRequestDTO req) {
        Evento e = new Evento();
        e.setTitle(req.getTitle());
        e.setDescription(req.getDescription());
        e.setParticipantCount(req.getParticipantCount());
        e.setOrganizerEmail(req.getOrganizerEmail());
        e.setCoHostEmail(req.getCoHostEmail());
        e.setRegistrationRequired(Boolean.TRUE.equals(req.getRegistrationRequired()));
        return e;
    }

    private LocalDateTime toUtc(OffsetDateTime odt) {
        if (odt == null) return null;
        return odt.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

private List<LocalDateTime> generateWeeklyStarts(LocalDateTime baseStart, br.com.reservasala.api.models.RecurrenceDTO r) {
    java.util.ArrayList<LocalDateTime> result = new java.util.ArrayList<>();
    int intervalWeeks = Math.max(1, r.getInterval());
    var baseDate = baseStart.toLocalDate();
    var baseTime = baseStart.toLocalTime();

    java.time.LocalDate endDate;
    int maxCount = Integer.MAX_VALUE;
    switch (r.getEndType()) {
        case UNTIL_DATE -> endDate = r.getUntilDate() != null ? r.getUntilDate() : baseDate.plusMonths(3);
        case AFTER_OCCURRENCES -> {
            maxCount = Math.max(1, r.getMaxOccurrences() != null ? r.getMaxOccurrences() : 1);
            endDate = baseDate.plusYears(1);
        }
        case NEVER -> endDate = baseDate.plusMonths(6);
        default -> endDate = baseDate.plusMonths(3);
    }

    java.time.LocalDate cursorWeekStart = baseDate;
    int count = 0;
    while (!cursorWeekStart.isAfter(endDate)) {
        for (Integer dow : r.getWeekDays()) {
            if (dow == null || dow < 1 || dow > 7) continue;
            java.time.LocalDate d = cursorWeekStart.with(java.time.DayOfWeek.of(dow));
            LocalDateTime candidate = LocalDateTime.of(d, baseTime);
            if (!candidate.isBefore(baseStart)) {
                result.add(candidate);
                if (++count >= maxCount) return result.stream().sorted().toList();
            }
        }
        cursorWeekStart = cursorWeekStart.plusWeeks(intervalWeeks);
    }
    result.sort(java.util.Comparator.naturalOrder());
    return result;
}

private Evento resolveSeriesRoot(Evento evento) {
    Evento current = evento;
    int guard = 0;
    while (current != null && current.getParentEventId() != null && guard < 20) {
        Evento parent = current.getParentEventId();
        if (parent.getId() == null) {
            current = parent;
            break;
        }
        current = eventoRepository.findById(parent.getId()).orElse(parent);
        guard++;
    }
    return current;
}

@GetMapping(value = "/{id}/start-url", produces = MediaType.APPLICATION_JSON_VALUE)
@PreAuthorize("hasAuthority('ROLE_ADMIN') or @eventoSecurityService.isOwner(authentication.name, #id)")
    public ResponseEntity<Map<String, String>> getFreshStartUrl(@PathVariable Long id) {
        return eventoRepository.findById(id)
            .map(evento -> {
                if (evento.getZoomMeetingId() == null) {
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Evento não possui meeting no Zoom."));
            }
            try {
                // Para recorrentes, use o occurrence_id da ocorrência específica (se houver)
                String occurrenceId = evento.getZoomOccurrenceId();
                String freshStartUrl = zoomService.getFreshStartUrl(evento.getZoomMeetingId(), occurrenceId);

                // opcional: persistir para auditoria/cache curto
                evento.setStartUrl(freshStartUrl);
                eventoRepository.save(evento);

                return ResponseEntity.ok(Map.of("startUrl", freshStartUrl));
            } catch (Exception e) {
                logger.error("Falha ao obter start_url para evento {}", evento.getId(), e);
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body(Map.of("error", "Falha ao contatar o Zoom para gerar start_url."));
            }
        })
        .orElse(ResponseEntity.notFound().build());
}

    @GetMapping(value = "/{id}/registrants", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @eventoSecurityService.isOwner(authentication.name, #id)")
    public ResponseEntity<?> getMeetingRegistrants(
            @PathVariable Long id,
            @RequestParam(value = "status", required = false) String status
    ) {
        return eventoRepository.findById(id)
                .map(evento -> {
                    if (!evento.isRegistrationRequired()) {
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(Map.of("error", "Este evento não exige inscrição prévia."));
                    }
                    if (evento.getZoomMeetingId() == null) {
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(Map.of("error", "Evento sem reunião Zoom vinculada."));
                    }

                    String occurrenceId = evento.getZoomOccurrenceId();
                    var registrants = zoomService.fetchMeetingRegistrants(evento.getZoomMeetingId(), occurrenceId, status);
                    return ResponseEntity.ok(registrants);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/{id}/report", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @eventoSecurityService.isOwner(authentication.name, #id)")
    public ResponseEntity<?> getReport(@PathVariable Long id) {
        return eventoReportService.getReport(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(value = "/{id}/report", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @eventoSecurityService.isOwner(authentication.name, #id)")
    @Transactional
    public ResponseEntity<?> generateReport(@PathVariable Long id) {
        try {
            var report = eventoReportService.generateReport(id);
            return ResponseEntity.ok(report);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Erro ao gerar relatório para evento {}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "Falha ao coletar dados do Zoom para o relatório."));
        }
    }

    @GetMapping(value = "/{id}/report/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @eventoSecurityService.isOwner(authentication.name, #id)")
    public ResponseEntity<byte[]> downloadReportPdf(@PathVariable Long id) {
        try {
            var reportOpt = eventoReportService.getReport(id);
            var report = reportOpt.orElseGet(() -> eventoReportService.generateReport(id));
            byte[] pdf = eventoReportService.buildPdf(id);
            if (pdf == null) return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"relatorio-reuniao.pdf\"")
                    .body(pdf);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            logger.error("Erro ao gerar/download PDF para evento {}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    private LocalDateTime nowBr() {
        return LocalDateTime.now(ZoneId.of("America/Sao_Paulo"));

    }


      @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public List<Evento> listarMeusFuturos(Authentication auth) {
        String username = auth.getName(); // deve casar com created_by_username
        return eventoRepository.findByCreatedByUsernameAndStatusAndStartTimeAfterOrderByStartTimeAsc(username, EventStatus.CONFIRMED, nowBr());
    }

    @GetMapping("/me/past")
    @PreAuthorize("isAuthenticated()")
    public List<Evento> listarMeusPassados(Authentication auth) {
        String username = auth.getName();
        return eventoRepository.findByCreatedByUsernameAndStatusAndStartTimeBeforeOrderByStartTimeDesc(username, EventStatus.CONFIRMED, nowBr());
    }

}
