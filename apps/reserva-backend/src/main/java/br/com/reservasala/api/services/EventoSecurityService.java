package br.com.reservasala.api.services;

import br.com.reservasala.api.models.Evento;
import br.com.reservasala.api.repository.EventoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("eventoSecurityService") // Damos um nome ao bean para usá-lo na anotação
public class EventoSecurityService {

    @Autowired
    private EventoRepository eventoRepository;

    public boolean isOwner(String username, Long eventoId) {
        Evento evento = eventoRepository.findById(eventoId).orElse(null);
        if (evento == null) {
            return false; // Evento não encontrado
        }
        return username != null
                && evento.getCreatedByUsername() != null
                && username.equalsIgnoreCase(evento.getCreatedByUsername());
    }
}
