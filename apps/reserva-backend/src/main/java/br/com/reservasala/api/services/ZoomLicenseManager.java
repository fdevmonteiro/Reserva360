package br.com.reservasala.api.services;

import java.time.LocalDateTime;
import java.util.List;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import br.com.reservasala.api.models.ZoomAccount;
import br.com.reservasala.api.repository.EventoRepository;
import br.com.reservasala.api.repository.ReservaRepository;
import br.com.reservasala.api.repository.ZoomAccountRepository;

@Service
public class ZoomLicenseManager {

    @Autowired
    private ZoomAccountRepository zoomAccountRepository;
    @Autowired
    private EventoRepository eventoRepository;
    @Autowired
    private ReservaRepository reservaRepository; // Injetar o novo repositório

    public ZoomAccount findAvailableAccount(OffsetDateTime startTime, OffsetDateTime endTime, int participantCount) {
        LocalDateTime startUtc = toUtc(startTime);
        LocalDateTime endUtc = toUtc(endTime);
        return findAvailableAccount(startUtc, endUtc, participantCount);
    }

    public ZoomAccount findAvailableAccount(LocalDateTime startTime, LocalDateTime endTime, int participantCount) {
        return findAvailableAccount(startTime, endTime, participantCount, null);
    }

    /**
     * Procura uma conta livre; se uma conta preferida for fornecida e estiver livre, ela é reaproveitada.
     */
    public ZoomAccount findAvailableAccount(LocalDateTime startTime, LocalDateTime endTime, int participantCount, ZoomAccount preferredAccount) {
        // 1. Pega as contas certas (grandes ou normais) ativas do banco
        List<ZoomAccount> accountsToSearch;
        if (participantCount > 300) {
            accountsToSearch = zoomAccountRepository.findAllByIsActiveAndIsLargeCapacity(true, true);
        } else {
            accountsToSearch = zoomAccountRepository.findAllByIsActive(true);
        }

        // 2. Pega os IDs das contas ocupadas em EVENTOS e RESERVAS
        List<Long> busyInEventos = eventoRepository.findBusyZoomAccountIds(startTime, endTime);
        List<Long> busyInReservas = reservaRepository.findBusyZoomAccountIdsInReservas(startTime, endTime);

        // 3. Combina as duas listas de IDs de contas ocupadas
        List<Long> busyAccountIds = Stream.concat(busyInEventos.stream(), busyInReservas.stream()).toList();

        // 4. Se houver uma conta preferida e ela não estiver ocupada, reaproveite
        if (preferredAccount != null) {
            boolean preferredListed = accountsToSearch.stream()
                    .anyMatch(acc -> acc.getId().equals(preferredAccount.getId()));
            if (preferredListed && !busyAccountIds.contains(preferredAccount.getId())) {
                return preferredAccount;
            }
        }

        // 5. Encontra a primeira conta da lista inicial que NÃO ESTÁ na lista de ocupadas
        return accountsToSearch.stream()
                .filter(account -> !busyAccountIds.contains(account.getId()))
                .findFirst()
                .orElse(null); // Retorna nulo se nenhuma estiver livre
    }

    private LocalDateTime toUtc(OffsetDateTime odt) {
        if (odt == null) return null;
        return odt.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    public boolean areAllSlotsAvailable(List<LocalDateTime> startTimes, LocalDateTime originalEndTime) {
        for (LocalDateTime startTime : startTimes) {
            // Calcula a hora de fim para esta ocorrência específica
            LocalDateTime endTime = originalEndTime.plusMinutes(startTime.getMinute() - originalEndTime.getMinute());
            
            if (findAvailableAccount(startTime, endTime, 0) == null) {
                // Se em qualquer uma das datas não houver uma conta livre, a verificação falha.
                return false;
            }
        }
        // Se o loop terminar, significa que há licenças para todas as datas.
        return true;
    }
}
