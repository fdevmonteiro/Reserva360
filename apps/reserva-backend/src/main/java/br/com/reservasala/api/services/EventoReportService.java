package br.com.reservasala.api.services;

import br.com.reservasala.api.models.Evento;
import br.com.reservasala.api.models.EventoReport;
import br.com.reservasala.api.models.EventoReportParticipant;
import br.com.reservasala.api.repository.EventoReportRepository;
import br.com.reservasala.api.repository.EventoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class EventoReportService {

    private static final Logger log = LoggerFactory.getLogger(EventoReportService.class);

    private final EventoRepository eventoRepository;
    private final EventoReportRepository reportRepository;
    private final ZoomService zoomService;
    private final ReportPdfService reportPdfService;
    private final EmailService emailService;

    public EventoReportService(EventoRepository eventoRepository, EventoReportRepository reportRepository, ZoomService zoomService, ReportPdfService reportPdfService, EmailService emailService) {
        this.eventoRepository = eventoRepository;
        this.reportRepository = reportRepository;
        this.zoomService = zoomService;
        this.reportPdfService = reportPdfService;
        this.emailService = emailService;
    }

    @Transactional
    public EventoReport generateReport(Long eventoId) {
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new IllegalArgumentException("Evento não encontrado: " + eventoId));

        if (evento.getZoomMeetingId() == null && (evento.getZoomMeetingUuid() == null || evento.getZoomMeetingUuid().isBlank())) {
            throw new IllegalStateException("Evento não possui meetingId/UUID do Zoom.");
        }

        // Para ocorrências de série recorrente sem UUID preenchido, o ID numérico compartilhado
        // não identifica a ocorrência específica na API do Zoom. Buscamos o UUID correto
        // listando as instâncias passadas e casando pelo horário de início.
        String meetingKey;
        if (evento.getZoomMeetingUuid() != null && !evento.getZoomMeetingUuid().isBlank()) {
            meetingKey = evento.getZoomMeetingUuid();
        } else if (evento.getParentEventId() != null && evento.getZoomMeetingId() != null) {
            String resolvedUuid = zoomService.findPastInstanceUuid(evento.getZoomMeetingId(), evento.getStartTime());
            if (resolvedUuid != null) {
                log.info("UUID resolvido para ocorrência {} da série: {}", evento.getId(), resolvedUuid);
                evento.setZoomMeetingUuid(resolvedUuid);
                eventoRepository.save(evento);
                meetingKey = resolvedUuid;
            } else {
                log.warn("Não foi possível resolver UUID para ocorrência recorrente {}, usando meetingId numérico", evento.getId());
                meetingKey = String.valueOf(evento.getZoomMeetingId());
            }
        } else {
            meetingKey = String.valueOf(evento.getZoomMeetingId());
        }

        ZoomService.PastMeetingReport zoomReport = zoomService.fetchPastMeetingReport(meetingKey);
        if (zoomReport == null || zoomReport.summary() == null) {
            throw new PastMeetingNotReadyException("Zoom não retornou informações de past_meeting para " + meetingKey);
        }

        EventoReport report = reportRepository.findByEventoId(eventoId).orElseGet(EventoReport::new);
        report.setEvento(evento);
        var summary = zoomReport.summary();
        report.setMeetingUuid(summary.meetingUuid());
        report.setTopic(summary.topic());
        report.setActualStartTime(summary.startTime());
        report.setActualEndTime(summary.endTime());
        report.setDurationMinutes(summary.durationMinutes());
        report.setTotalParticipants(summary.participantsCount());
        report.setPeakParticipants(summary.peakParticipants());
        report.setTotalMinutes(summary.totalMinutes());
        report.setRecordingUrl(summary.recordingUrl());
        report.setRecordingStatus(summary.recordingStatus());
        report.setGeneratedAt(LocalDateTime.now());

        // Atualiza participantes (limpa e recria)
        List<EventoReportParticipant> participants = report.getParticipants();
        if (participants == null) {
            participants = new java.util.ArrayList<>();
            report.setParticipants(participants);
        } else {
            participants.clear();
        }
        for (ZoomService.PastParticipant p : zoomReport.participants()) {
            EventoReportParticipant part = new EventoReportParticipant();
            part.setReport(report);
            part.setName(p.name());
            part.setEmail(p.email());
            part.setJoinTime(p.joinTime());
            part.setLeaveTime(p.leaveTime());
            part.setDurationMinutes(p.durationMinutes());
            participants.add(part);
        }

        // Garante que UUID fique salvo no evento para futuras consultas
        if (summary.meetingUuid() != null && (evento.getZoomMeetingUuid() == null || evento.getZoomMeetingUuid().isBlank())) {
            evento.setZoomMeetingUuid(summary.meetingUuid());
            eventoRepository.save(evento);
        }

        EventoReport saved = reportRepository.save(report);
        sendReportIfNeeded(evento, saved);
        markGenerated(evento);
        return saved;
    }

    public Optional<EventoReport> getReport(Long eventoId) {
        return reportRepository.findByEventoId(eventoId);
    }

    /** Marca o evento como já processado para evitar duplicidade */
    private void markGenerated(Evento evento) {
        if (!evento.isReportGenerated()) {
            evento.setReportGenerated(true);
            eventoRepository.save(evento);
        }
    }

    private void sendReportIfNeeded(Evento evento, EventoReport report) {
        if (report.isEmailed()) return;
        try {
            byte[] pdf = reportPdfService.buildReportPdf(evento, report);
            emailService.sendEventReportEmail(evento, report, pdf);
            report.setEmailed(true);
            report.setEmailedAt(LocalDateTime.now());
            reportRepository.save(report);
        } catch (Exception e) {
            log.warn("Falha ao enviar relatório por e-mail para evento {}: {}", evento.getId(), e.getMessage());
        }
    }

    public byte[] buildPdf(Long eventoId) {
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new IllegalArgumentException("Evento não encontrado: " + eventoId));
        EventoReport report = reportRepository.findByEventoId(eventoId)
                .orElseThrow(() -> new IllegalArgumentException("Relatório não encontrado para o evento: " + eventoId));
        return reportPdfService.buildReportPdf(evento, report);
    }

    /** Exceção sem stack para sinalizar que o Zoom ainda não liberou o past_meeting */
    public static class PastMeetingNotReadyException extends RuntimeException {
        public PastMeetingNotReadyException(String message) { super(message, null, false, false); }
    }
}
