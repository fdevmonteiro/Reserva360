package br.com.reservasala.api.controllers;

import br.com.reservasala.api.models.InscricaoEvento;
import br.com.reservasala.api.models.InscricaoRequestDTO;
import br.com.reservasala.api.services.InscricaoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inscricoes")
public class InscricaoController {

    private final InscricaoService inscricaoService;

    public InscricaoController(InscricaoService inscricaoService) {
        this.inscricaoService = inscricaoService;
    }

    /** Retorna informações públicas do evento para a página de inscrição. */
    @GetMapping("/evento/{eventoId}/info")
    public ResponseEntity<?> getEventoInfo(@PathVariable Long eventoId) {
        try {
            Map<String, Object> info = inscricaoService.getEventoInfo(eventoId);
            return ResponseEntity.ok(info);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    /** Realiza inscrição no evento (público, sem autenticação). */
    @PostMapping("/evento/{eventoId}")
    public ResponseEntity<?> inscrever(
            @PathVariable Long eventoId,
            @RequestBody @Valid InscricaoRequestDTO dto) {
        try {
            Map<String, Object> result = inscricaoService.inscrever(eventoId, dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    /** Lista todos os inscritos de um evento (requer autenticação). */
    @GetMapping("/evento/{eventoId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> listarInscritos(@PathVariable Long eventoId) {
        try {
            List<InscricaoEvento> inscritos = inscricaoService.listarPorEvento(eventoId);
            return ResponseEntity.ok(inscritos);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    /** Retorna a contagem de inscritos de um evento (requer autenticação). */
    @GetMapping("/evento/{eventoId}/count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Long>> contarInscritos(@PathVariable Long eventoId) {
        long count = inscricaoService.contarPorEvento(eventoId);
        return ResponseEntity.ok(Map.of("total", count));
    }

    /** Exporta a planilha Excel com inscritos (requer autenticação). */
    @GetMapping("/evento/{eventoId}/export/excel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> exportarExcel(@PathVariable Long eventoId) {
        try {
            byte[] bytes = inscricaoService.exportarExcel(eventoId);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"inscritos-evento-" + eventoId + ".xlsx\"")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
