package br.com.reservasala.api.controllers;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.reservasala.api.models.EventoDashboardStatsDTO;
import br.com.reservasala.api.repository.EventoRepository;
import br.com.reservasala.api.models.EventStatus;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    
    @Autowired private EventoRepository eventoRepository;

     @GetMapping("/event-stats")
    public EventoDashboardStatsDTO getEventStats() {
        EventoDashboardStatsDTO stats = new EventoDashboardStatsDTO();
        LocalDateTime now = LocalDateTime.now();
        stats.setTotalEventosFuturos(eventoRepository.countByStatusAndStartTimeAfter(EventStatus.CONFIRMED, now));
        stats.setTotalOrganizadores(eventoRepository.countDistinctOrganizerEmailByStatusAndStartTimeAfter(EventStatus.CONFIRMED, now));
        stats.setTotalEventosPassados(eventoRepository.countByStatusAndStartTimeBefore(EventStatus.CONFIRMED, now));
        

        return stats;

    }
}

