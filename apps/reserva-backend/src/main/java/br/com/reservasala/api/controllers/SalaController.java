package br.com.reservasala.api.controllers;

import br.com.reservasala.api.models.Sala;
import br.com.reservasala.api.repository.SalaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/salas")
public class SalaController {

    @Autowired
    private SalaRepository salaRepository;

   
    @GetMapping
    public List<Sala> getAllSalas() {
        return salaRepository.findAll();
    }

  
    @GetMapping("/disponiveis")
    public List<Sala> getAvailableSalas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        
        return salaRepository.findAvailableSalas(startTime, endTime);
    }
}