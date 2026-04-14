package br.com.reservasala.api.repository;

import br.com.reservasala.api.models.EventoReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EventoReportRepository extends JpaRepository<EventoReport, Long> {
    Optional<EventoReport> findByEventoId(Long eventoId);
}
