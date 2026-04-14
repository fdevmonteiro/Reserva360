package br.com.reservasala.api.services;

import br.com.reservasala.api.models.Evento;
import br.com.reservasala.api.models.EventStatus;
import br.com.reservasala.api.models.EventoRequestDTO;
import br.com.reservasala.api.repository.EventoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class EventoService {

    private static final Logger log = LoggerFactory.getLogger(EventoService.class);

    private final EventoRepository eventoRepository;
    private final EmailService emailService;

    public EventoService(EventoRepository eventoRepository, EmailService emailService) {
        this.eventoRepository = eventoRepository;
        this.emailService = emailService;
    }

    /**
     * Cria série recorrente no estado PENDING:
     * - Cria o evento pai com token único
     * - Cria os filhos com o MESMO token e parentEventId = pai
     * - NÃO reserva licença aqui (será verificada na confirmação)
     * - Envia UM e-mail (do pai) com link para confirmar a série
     *
     * @param req DTO base (title/description/organizer etc.)
     * @param starts lista de inícios (cada ocorrência)
     * @return evento pai criado
     */
    @Transactional
    public Evento createRecurringEvents(EventoRequestDTO req, List<LocalDateTime> starts, String createdByUsername) {
        if (starts == null || starts.isEmpty()) {
            throw new IllegalArgumentException("Lista de ocorrências vazia");
        }
        if (!toUtc(req.getStartTime()).isBefore(toUtc(req.getEndTime()))) {
            throw new IllegalArgumentException("'startTime' deve ser antes de 'endTime'");
        }

        // duração fixa baseada no DTO base
        Duration duration = Duration.between(toUtc(req.getStartTime()), toUtc(req.getEndTime()));

        // token único para a série
        String seriesToken = UUID.randomUUID().toString();

        // Pai: reflete a primeira ocorrência
        Evento parent = new Evento();
        parent.setTitle(req.getTitle());
        parent.setDescription(req.getDescription());
        parent.setStartTime(starts.get(0));
        parent.setEndTime(starts.get(0).plus(duration));
        parent.setParticipantCount(req.getParticipantCount());
        parent.setOrganizerEmail(req.getOrganizerEmail());
        parent.setCoHostEmail(req.getCoHostEmail());
        parent.setStatus(EventStatus.PENDING);
        parent.setConfirmationToken(seriesToken);
        parent.setCreatedByUsername(createdByUsername);
        // hostZoomAccount = null aqui; será definido/revalidado na confirmação
        parent = eventoRepository.save(parent);

        // Filhos
        for (int i = 0; i < starts.size(); i++) {
            // já criamos o pai acima
            if (i == 0) continue;

            LocalDateTime s = starts.get(i);
            Evento child = new Evento();
            child.setTitle(req.getTitle());
            child.setDescription(req.getDescription());
            child.setStartTime(s);
            child.setEndTime(s.plus(duration));
            child.setParticipantCount(req.getParticipantCount());
            child.setOrganizerEmail(req.getOrganizerEmail());
            child.setCoHostEmail(req.getCoHostEmail());
            child.setStatus(EventStatus.PENDING);
            child.setConfirmationToken(seriesToken);
            child.setCreatedByUsername(createdByUsername);
            child.setParentEventId(parent); // <<< relacionamento com o pai
            eventoRepository.save(child);
        }

        // ÚNICO e-mail de confirmação (do pai) com o token da série
        try {
            emailService.sendSeriesConfirmationEmail(parent);
        } catch (Exception e) {
            log.warn("Falha ao enviar e-mail de confirmação da série {}: {}", parent.getId(), e.getMessage());
        }

        return parent;
    }

    private LocalDateTime toUtc(OffsetDateTime odt) {
        if (odt == null) return null;
        return odt.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }
}
