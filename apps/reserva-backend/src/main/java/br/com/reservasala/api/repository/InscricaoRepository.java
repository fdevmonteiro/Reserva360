package br.com.reservasala.api.repository;

import br.com.reservasala.api.models.InscricaoEvento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InscricaoRepository extends JpaRepository<InscricaoEvento, Long> {

    List<InscricaoEvento> findByEventoIdOrderByCreatedAtDesc(Long eventoId);

    boolean existsByEventoIdAndEmail(Long eventoId, String email);

    long countByEventoId(Long eventoId);
}
