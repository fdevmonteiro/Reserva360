package br.com.reservasala.api.repository;

import br.com.reservasala.api.models.Evento;
import br.com.reservasala.api.models.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventoRepository extends JpaRepository<Evento, Long> {


    List<Evento> findByCreatedByUsernameAndStatusAndStartTimeAfterOrderByStartTimeAsc(String createdByUsername, EventStatus status,  LocalDateTime now);
    List<Evento> findByCreatedByUsernameAndStatusAndStartTimeBeforeOrderByStartTimeDesc(String createdByUsername, EventStatus status,  LocalDateTime now);

    
    // --- MÉTODOS PARA O FLUXO DE CONFIRMAÇÃO ---
    

     List<Evento> findByStatusAndStartTimeAfterOrderByStartTimeAsc(EventStatus status, LocalDateTime now);

     
    /**
     * Encontra um evento pelo seu token de confirmação único.
     */
    Optional<Evento> findByConfirmationToken(String token);

    /**
     * Busca todos os eventos com um status específico, ordenados por data de início.
     */
    List<Evento> findByStatusOrderByStartTimeAsc(EventStatus status);


    // --- MÉTODOS PARA O DASHBOARD E PÁGINA INICIAL ---

    /**
     * Conta o número de eventos futuros que estão com um status específico.
     */
    long countByStatusAndStartTimeAfter(EventStatus status, LocalDateTime now);

    /**
     * Conta o número de eventos passados que estão com um status específico.
     */
    long countByStatusAndStartTimeBefore(EventStatus status, LocalDateTime now);

    /**
     * Conta o número de organizadores distintos para eventos futuros com um status específico.
     */
    @Query("SELECT COUNT(DISTINCT e.organizerEmail) FROM Evento e WHERE e.startTime >= :now AND e.status = :status")
    Long countDistinctOrganizerEmailByStatusAndStartTimeAfter(
        @Param("status") EventStatus status,
        @Param("now") LocalDateTime now
    );

    /**
     * Busca os 5 próximos eventos com um status específico, ordenados por data.
     */
    List<Evento> findTop5ByStatusAndStartTimeAfterOrderByStartTimeAsc(EventStatus status, LocalDateTime now);

    /**
     * Busca os eventos passados com um status específico, ordenados por data.
     */
    List<Evento> findByStatusAndStartTimeBeforeOrderByStartTimeDesc(EventStatus status, LocalDateTime now);

    @Query("SELECT e.hostZoomAccount.id FROM Evento e " +
           "WHERE e.status = 'CONFIRMED' " +
           "AND e.hostZoomAccount IS NOT NULL " +
           "AND (e.startTime < :endTime AND e.endTime > :startTime)")
    List<Long> findBusyZoomAccountIds(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    List<Evento> findByParentEventId(Evento parentEventId);

    Optional<Evento> findByZoomMeetingUuid(String zoomMeetingUuid);
    Optional<Evento> findByZoomMeetingId(Long zoomMeetingId);

    List<Evento> findTop50ByStatusAndReportGeneratedFalseAndEndTimeBeforeOrderByEndTimeAsc(
            EventStatus status, LocalDateTime before);
}
