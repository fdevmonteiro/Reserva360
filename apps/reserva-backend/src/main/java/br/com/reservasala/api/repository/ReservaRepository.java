package br.com.reservasala.api.repository;

import br.com.reservasala.api.models.Reserva;
import br.com.reservasala.api.models.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    
    Optional<Reserva> findByConfirmationToken(String token);

    List<Reserva> findByStatus(ReservationStatus status);
    Page<Reserva> findByStatus(ReservationStatus status, Pageable pageable);

    List<Reserva> findByStatusAndSalaNomeContainingIgnoreCase(ReservationStatus status, String salaNome);
    Page<Reserva> findByStatusAndSalaNomeContainingIgnoreCase(ReservationStatus status, String salaNome, Pageable pageable);

    List<Reserva> findByStatusAndSalaNomeContainingIgnoreCaseAndStartTimeBetween(
            ReservationStatus status,
            String salaNome,
            LocalDateTime start,
            LocalDateTime end
    );
    Page<Reserva> findByStatusAndSalaNomeContainingIgnoreCaseAndStartTimeBetween(
            ReservationStatus status,
            String salaNome,
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    );


    List<Reserva> findByStatusAndCreatedByUsernameOrderByStartTimeAsc(ReservationStatus status, String username);
    
 Optional<Reserva> findByIdAndCreatedByUsername(Long id, String createdByUsername);
 
 List<Reserva> findTop5ByStatusAndStartTimeAfterOrderByStartTimeAsc(ReservationStatus status, LocalDateTime now);
 
    List<Reserva> findByStatusAndStartTimeBetween(ReservationStatus status, LocalDateTime start, LocalDateTime end);
    Page<Reserva> findByStatusAndStartTimeBetween(ReservationStatus status, LocalDateTime start, LocalDateTime end, Pageable pageable);

    @Query("SELECT r.hostZoomAccount.id FROM Reserva r " +
           "WHERE r.status = 'CONFIRMED' " +
           "AND r.createZoomMeeting = true " +
           "AND r.hostZoomAccount IS NOT NULL " +
           "AND (r.startTime < :endTime AND r.endTime > :startTime)")
    List<Long> findBusyZoomAccountIdsInReservas(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );
}
