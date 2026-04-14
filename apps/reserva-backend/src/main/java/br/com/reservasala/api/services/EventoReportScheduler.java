package br.com.reservasala.api.services;

import br.com.reservasala.api.models.EventStatus;
import br.com.reservasala.api.models.Evento;
import br.com.reservasala.api.repository.EventoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class EventoReportScheduler {

    private static final Logger log = LoggerFactory.getLogger(EventoReportScheduler.class);

    private final EventoRepository eventoRepository;
    private final EventoReportService eventoReportService;
    private final Clock clock;
    private final boolean autoEnabled;
    private final int cutoffMinutes;
    private final int batchSize;

    public EventoReportScheduler(
            EventoRepository eventoRepository,
            EventoReportService eventoReportService,
            Clock clock,
            @Value("${reports.auto.enabled:true}") boolean autoEnabled,
            @Value("${reports.auto.cutoff-minutes:5}") int cutoffMinutes,
            @Value("${reports.auto.batch-size:20}") int batchSize
    ) {
        this.eventoRepository = eventoRepository;
        this.eventoReportService = eventoReportService;
        this.clock = clock;
        this.autoEnabled = autoEnabled;
        this.cutoffMinutes = cutoffMinutes;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${reports.auto.fixed-delay-ms:300000}", initialDelayString = "${reports.auto.initial-delay-ms:60000}")
    public void generatePendingReports() {
        if (!autoEnabled) return;

        LocalDateTime limit = LocalDateTime.now(clock).minusMinutes(cutoffMinutes);
        List<Evento> pendings = eventoRepository
                .findTop50ByStatusAndReportGeneratedFalseAndEndTimeBeforeOrderByEndTimeAsc(
                        EventStatus.CONFIRMED, limit
                );

        if (pendings.isEmpty()) return;

        log.info("Scheduler: gerando relatórios para {} eventos (limite {})", pendings.size(), limit);
        int processed = 0;
        for (Evento e : pendings) {
            if (processed >= batchSize) break;
            try {
                eventoReportService.generateReport(e.getId());
                processed++;
            } catch (EventoReportService.PastMeetingNotReadyException ex) {
                log.info("Scheduler: relatório ainda não disponível para evento {} ({}", e.getId(), ex.getMessage());
            } catch (Exception ex) {
                log.warn("Scheduler: falha ao gerar relatório do evento {}: {}", e.getId(), ex.getMessage());
            }
        }
    }
}
