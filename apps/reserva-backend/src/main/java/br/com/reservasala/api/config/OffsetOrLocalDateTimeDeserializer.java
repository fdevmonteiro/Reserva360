package br.com.reservasala.api.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Deserializa OffsetDateTime aceitando strings sem offset.
 * Quando não houver offset explícito, assume o fuso de negócio (America/Sao_Paulo).
 */
public class OffsetOrLocalDateTimeDeserializer extends JsonDeserializer<OffsetDateTime> {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");

    @Override
    public OffsetDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getText();
        if (text == null || text.isBlank()) return null;
        try {
            // Tenta parse direto com offset (Z ou +/-)
            return OffsetDateTime.parse(text, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch (DateTimeParseException ignored) {
            // Se não tiver offset, assume horário local de negócio e converte para offset correspondente
            try {
                LocalDateTime ldt = LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                return ldt.atZone(BUSINESS_ZONE).toOffsetDateTime();
            } catch (DateTimeParseException e) {
                throw new IOException("Data/hora inválida: " + text, e);
            }
        }
    }
}
