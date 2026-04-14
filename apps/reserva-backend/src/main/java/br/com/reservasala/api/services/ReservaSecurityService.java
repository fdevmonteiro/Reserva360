package br.com.reservasala.api.services;

import br.com.reservasala.api.models.Reserva;
import br.com.reservasala.api.repository.ReservaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("reservaSecurityService") // Damos um nome ao bean para usá-lo na anotação
public class ReservaSecurityService {

    @Autowired
    private ReservaRepository reservaRepository;

    /**
     * Verifica se o utilizador logado é o dono de uma reserva específica.
     * @param username O nome do utilizador atualmente logado.
     * @param reservaId O ID da reserva que ele está a tentar aceder.
     * @return true se o utilizador for o dono, false caso contrário.
     */
    public boolean isOwner(String username, Long reservaId) {
        Reserva reserva = reservaRepository.findById(reservaId).orElse(null);
        if (reserva == null) {
            return false; // Reserva não encontrada
        }
        return username != null && username.equals(reserva.getCreatedByUsername());
    }
}