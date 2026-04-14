package br.com.reservasala.api.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import br.com.reservasala.api.models.Reserva;
import br.com.reservasala.api.models.ReservationStatus;
import br.com.reservasala.api.models.ZoomAccount;
import br.com.reservasala.api.repository.ReservaRepository;
import br.com.reservasala.api.services.EmailService;
import br.com.reservasala.api.services.GraphApiService;
import br.com.reservasala.api.services.ZoomLicenseManager;
import br.com.reservasala.api.services.ZoomService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import br.com.reservasala.api.controllers.EventoController;



@RestController
@RequestMapping({"/api/reservas", "/reservas" })
public class ReservaController {

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private EmailService emailService;
    @Autowired
    private GraphApiService graphApiService;

    @Autowired
    private ZoomService zoomService;

    @Autowired
    private ZoomLicenseManager zoomLicenseManager;

    @Autowired
    private EventoController eventoController;

    private static final Logger log = LoggerFactory.getLogger(ReservaController.class);

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Reserva> createReserva(@RequestBody Reserva reserva) {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        reserva.setCreatedByUsername(currentUsername);
    ;
        reserva.setStatus(ReservationStatus.PENDING);
        reserva.setConfirmationToken(UUID.randomUUID().toString());
        
        Reserva savedReserva = reservaRepository.save(reserva);
        
        emailService.sendReservationConfirmationEmail(savedReserva);

        return new ResponseEntity<>(savedReserva, HttpStatus.CREATED);
    }

   @GetMapping(path = "/{id}/start-url", produces = MediaType.APPLICATION_JSON_VALUE)
@PreAuthorize("isAuthenticated()") // checamos dono abaixo
public ResponseEntity<Map<String, String>> getStartUrl(
        @PathVariable("id") Long id,
        Authentication auth) throws Exception {

    final String who = (auth != null ? auth.getName() : "ANON");
    log.info(">> HIT start-url: id={}, user={}", id, who);

    Reserva r = reservaRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva não encontrada."));

    boolean isAdmin = auth.getAuthorities().stream()
        .map(a -> a.getAuthority())
        .anyMatch(role ->
                "ROLE_ADMIN".equals(role)
             || "ROLE_G.TI".equals(role)
             || "ADMIN".equals(role)
             || "G.TI".equals(role)
        );

    boolean isOwner = r.getCreatedByUsername() != null
            && who != null
            && r.getCreatedByUsername().equalsIgnoreCase(who);

    if (!isAdmin && !isOwner) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não é o dono desta reserva.");
    }

    if (r.getZoomMeetingId() == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reserva não possui reunião Zoom.");
    }

    String hostEmail = (r.getHostZoomAccount() != null ? r.getHostZoomAccount().getAccountMail() : null);

    String startUrl;
    try {
        startUrl = zoomService.getFreshStartUrl(r.getZoomMeetingId(), hostEmail);
        if (startUrl != null && !startUrl.isBlank() &&
            (r.getStartUrl() == null || !startUrl.equals(r.getStartUrl()))) {
            r.setStartUrl(startUrl);
            reservaRepository.save(r);
        }
    } catch (RuntimeException ex) {
        log.warn("Falha ao renovar start_url no Zoom: {}", ex.getMessage());
        startUrl = r.getStartUrl();
    }

    if (startUrl == null || startUrl.isBlank()) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Não foi possível obter o start_url.");
    }

    return ResponseEntity.ok(Map.of("startUrl", startUrl));
}


    @GetMapping("/confirmar")
    public ResponseEntity<String> confirmReservation(@RequestParam("token") String token) {
    return reservaRepository.findByConfirmationToken(token)
        .map(reserva -> {
            reserva.setStatus(ReservationStatus.CONFIRMED);
            reserva.setConfirmationToken(null);
            
            String successMessage = "<h1>Reserva Confirmada!</h1><p>A sua reserva foi confirmada com sucesso.</p>";

            // A lógica do Zoom só é executada se o utilizador pediu
            if (reserva.isCreateZoomMeeting()) {
                // 1. Encontra uma licença livre, passando o número de participantes da reserva
                ZoomAccount availableAccount = zoomLicenseManager.findAvailableAccount(
                    reserva.getStartTime(), 
                    reserva.getEndTime(),
                    reserva.getParticipantCount() // Passa a contagem de participantes
                );

                if (availableAccount == null) {
                    // Se não houver licença, avisa o utilizador
                    successMessage += "<p style='color:red;'><b>Aviso:</b> A sala foi reservada, mas não foi possível criar a reunião no Zoom pois não há licenças disponíveis para este horário.</p>";
                } else {
                    // Se houver licença, associa-a à reserva
                    reserva.setHostZoomAccount(availableAccount);
                    
                    // Chama o serviço do Zoom para criar a reunião
                    Map<String, Object> zoomDetails = zoomService.createMeetingForReserva(reserva, availableAccount);
                    
                    if (zoomDetails != null) {
                        reserva.setLink((String) zoomDetails.get("url"));
                        reserva.setZoomMeetingId((Long) zoomDetails.get("id"));
                        String zoomPasscode = (zoomDetails.containsKey("passcode")) ? (String) zoomDetails.get("passcode") : "";
                        
                        // Envia o email separado com os detalhes do Zoom
                        emailService.sendReservationZoomDetailsEmail(reserva, zoomPasscode);
                        successMessage += "<p>Um email separado com os detalhes da reunião do Zoom foi enviado.</p>";
                    }
                }
            }
            
            Reserva finalReserva = reservaRepository.save(reserva);

            // Cria o evento no calendário do Microsoft 365
            try {
                graphApiService.createCalendarEventForReserva(finalReserva.getOrganizerEmail(), finalReserva);
            } catch (Exception e) {
                System.err.println("AVISO: Erro ao criar evento de calendário para reserva: " + e.getMessage());
            }

            return ResponseEntity.ok(successMessage);
        })
        .orElse(ResponseEntity.badRequest().body("<h1>Token Inválido!</h1>"));
}

   
    @GetMapping("/cancelar")
    public ResponseEntity<String> cancelReservation(@RequestParam("token") String token) {
        return reservaRepository.findByConfirmationToken(token)
            .map(reserva -> {
                reserva.setStatus(ReservationStatus.CANCELED);
                reserva.setConfirmationToken(null);
                reservaRepository.save(reserva);
                return ResponseEntity.ok("<h1>Reserva Cancelada</h1><p>A reserva foi cancelada com sucesso. Obrigado por nos avisar.</p>");
            })
            .orElse(ResponseEntity.badRequest().body("<h1>Token Inválido!</h1><p>Este link de cancelamento é inválido ou já foi utilizado.</p>"));
    }

    @GetMapping
    public Page<Reserva> getAllReservas(
            @RequestParam(required = false) String salaNome,
            @RequestParam(required = false) String period,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        LocalDateTime startTime = LocalDate.now().atStartOfDay();
        LocalDateTime endTime = null;

        if (period != null) {
            switch (period) {
                case "7days":
                    endTime = startTime.plusDays(7).withHour(23).withMinute(59).withSecond(59);
                    break;
                case "1month":
                    endTime = startTime.plusMonths(1).withHour(23).withMinute(59).withSecond(59);
                    break;
                case "6months":
                    endTime = startTime.plusMonths(6).withHour(23).withMinute(59).withSecond(59);
                    break;
            }
        }

        PageRequest pageable = PageRequest.of(page, size, Sort.by("startTime").ascending());
        boolean hasSalaNome = salaNome != null && !salaNome.isEmpty();
        boolean hasPeriod = endTime != null;

        if (hasSalaNome && hasPeriod) {
            return reservaRepository.findByStatusAndSalaNomeContainingIgnoreCaseAndStartTimeBetween(
                    ReservationStatus.CONFIRMED, salaNome, startTime, endTime, pageable);
        } else if (hasSalaNome) {
            return reservaRepository.findByStatusAndSalaNomeContainingIgnoreCase(
                    ReservationStatus.CONFIRMED, salaNome, pageable);
        } else if (hasPeriod) {
            return reservaRepository.findByStatusAndStartTimeBetween(
                    ReservationStatus.CONFIRMED, startTime, endTime, pageable);
        } else {
            return reservaRepository.findByStatus(ReservationStatus.CONFIRMED, pageable);
        }
    }

    
    @DeleteMapping("/{id}")
@PreAuthorize("hasAuthority('ROLE_ADMIN') or @reservaSecurityService.isOwner(authentication.name, #id) ")
    public ResponseEntity<Void> deleteReserva(@PathVariable Long id) {
        reservaRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @reservaSecurityService.isOwner(authentication.name, #id) ")
    public ResponseEntity <Reserva> updateReserva(@PathVariable Long id, @RequestBody  Reserva reservaDetails) {
       
      Optional<Reserva> optionalReserva = reservaRepository.findById(id);

      if(optionalReserva.isPresent()){
        Reserva existingReserva = optionalReserva.get();

        existingReserva.setTitle(reservaDetails.getTitle());
            existingReserva.setDescription(reservaDetails.getDescription());
            existingReserva.setStartTime(reservaDetails.getStartTime());
            existingReserva.setEndTime(reservaDetails.getEndTime());
            existingReserva.setOrganizerEmail(reservaDetails.getOrganizerEmail());
            
            existingReserva.setSala(reservaDetails.getSala());

              final Reserva updatedReserva = reservaRepository.save(existingReserva);
            return ResponseEntity.ok(updatedReserva);
        } else {
          
            return ResponseEntity.notFound().build();
      }
    
  
    }
    
    @GetMapping("/proximas")
public List<Reserva> getUpcomingReservas() {
    return reservaRepository.findTop5ByStatusAndStartTimeAfterOrderByStartTimeAsc(
        ReservationStatus.CONFIRMED, 
        LocalDateTime.now()
    );
}

 @GetMapping("/minhas")
    public List<Reserva> getMyReservas() {
        // Pega o nome do utilizador que está autenticado
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        // Usa o novo método do repositório para buscar as reservas
        return reservaRepository.findByStatusAndCreatedByUsernameOrderByStartTimeAsc(
            ReservationStatus.CONFIRMED, 
            currentUsername
        );
    }

}
