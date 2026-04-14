package br.com.reservasala.api.controllers;

import br.com.reservasala.api.models.Evento;
import br.com.reservasala.api.repository.EventoRepository;
import br.com.reservasala.api.services.EventoReportService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/zoom/webhook")
public class ZoomWebhookController {

    private static final Logger log = LoggerFactory.getLogger(ZoomWebhookController.class);

    private final ObjectMapper mapper = new ObjectMapper();
    private final EventoRepository eventoRepository;
    private final EventoReportService eventoReportService;
    private final String verificationToken;

    public ZoomWebhookController(
            EventoRepository eventoRepository,
            EventoReportService eventoReportService,
            @Value("${zoom.webhook.verification-token:}") String verificationToken
    ) {
        this.eventoRepository = eventoRepository;
        this.eventoReportService = eventoReportService;
        this.verificationToken = verificationToken;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<?> handle(@RequestBody String body, @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (verificationToken == null || verificationToken.isBlank()) {
            log.error("zoom.webhook.verification-token não configurado — requisição rejeitada");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("webhook não configurado");
        }
        if (authHeader == null || !authHeader.equals(verificationToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("invalid token");
        }

        try {
            JsonNode json = mapper.readTree(body);
            String event = json.path("event").asText(null);
            JsonNode object = json.path("payload").path("object");
            String uuid = object.path("uuid").asText(null);
            Long meetingId = object.hasNonNull("id") ? object.get("id").asLong() : null;

            // Armazena o UUID se vier url-encoded (Zoom envia com //); deixar como veio para consulta
            String decodedUuid = uuid;
            if (uuid != null) {
                try {
                    decodedUuid = java.net.URLDecoder.decode(uuid, java.nio.charset.StandardCharsets.UTF_8);
                } catch (Exception ignored) {}
            }

            Optional<Evento> evOpt = Optional.empty();
            if (decodedUuid != null) {
                evOpt = eventoRepository.findByZoomMeetingUuid(decodedUuid);
            }
            if (evOpt.isEmpty() && meetingId != null) {
                evOpt = eventoRepository.findByZoomMeetingId(meetingId);
            }

            if (evOpt.isEmpty()) {
                log.warn("Webhook {} sem evento correspondente. uuid={}, id={}", event, decodedUuid, meetingId);
                return ResponseEntity.ok().build();
            }

            Evento evento = evOpt.get();
            if (decodedUuid != null && (evento.getZoomMeetingUuid() == null || evento.getZoomMeetingUuid().isBlank())) {
                evento.setZoomMeetingUuid(decodedUuid);
                eventoRepository.save(evento);
            }

            if ("meeting.ended".equalsIgnoreCase(event) || "recording.completed".equalsIgnoreCase(event)) {
                try {
                    eventoReportService.generateReport(evento.getId());
                    return ResponseEntity.ok().body(java.util.Map.of("status", "report-generated"));
                } catch (Exception e) {
                    log.error("Falha ao gerar relatório via webhook para evento {}", evento.getId(), e);
                    return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                            .body(java.util.Map.of("error", "falha ao gerar relatório"));
                }
            }

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Erro ao processar webhook do Zoom", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("payload inválido");
        }
    }
}
