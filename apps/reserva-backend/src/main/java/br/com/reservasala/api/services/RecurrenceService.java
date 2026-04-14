package br.com.reservasala.api.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class RecurrenceService {

 public List<LocalDateTime> calculateRecurrenceDates(LocalDateTime firstOccurrence, String frequency, LocalDate untilDate) {
        List<LocalDateTime> allDates = new ArrayList<>();
        
        // A primeira ocorrência é sempre a data de início que o usuário escolheu.
        allDates.add(firstOccurrence);

        LocalDateTime nextOccurrence = firstOccurrence;

        // Loop para calcular as datas futuras.
        while (true) {
            // Calcula a próxima data com base na frequência.
            switch (frequency.toUpperCase()) {
                case "WEEKLY":
                    nextOccurrence = nextOccurrence.plusWeeks(1);
                    break;
                case "MONTHLY":
                    nextOccurrence = nextOccurrence.plusMonths(1);
                    break;
                default:
                    // Se a frequência for desconhecida, para o loop.
                    return allDates;
            }

            // Verifica se a próxima data calculada ainda está dentro do período de repetição.
            if (nextOccurrence.toLocalDate().isAfter(untilDate)) {
                break; // Se passou da data final, para o loop.
            }

            // Se a data for válida, adiciona à lista.
            allDates.add(nextOccurrence);
        }

        return allDates;
    }
}


