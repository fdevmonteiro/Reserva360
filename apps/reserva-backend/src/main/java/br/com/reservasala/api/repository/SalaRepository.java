package br.com.reservasala.api.repository;

import br.com.reservasala.api.models.Sala;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SalaRepository extends JpaRepository<Sala, Long> {


    
    @Query("SELECT s FROM Sala s WHERE s.id NOT IN (" +
           "SELECT r.sala.id FROM Reserva r WHERE r.status = 'CONFIRMED' AND (" +
           "(r.startTime < :endTime AND r.endTime > :startTime)" +
           "))")
    List<Sala> findAvailableSalas(
        @Param("startTime") LocalDateTime startTime, 
        @Param("endTime") LocalDateTime endTime
   
        );


     
}
