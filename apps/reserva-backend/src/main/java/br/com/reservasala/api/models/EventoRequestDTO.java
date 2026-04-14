package br.com.reservasala.api.models;

import br.com.reservasala.api.config.OffsetOrLocalDateTimeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.OffsetDateTime;

public class EventoRequestDTO {

    private String title;
    private String description;
    // Espera ISO 8601 com offset/UTC (ex.: 2025-01-01T10:00:00-03:00 ou ...Z)
    @JsonDeserialize(using = OffsetOrLocalDateTimeDeserializer.class)
    private OffsetDateTime startTime;
    @JsonDeserialize(using = OffsetOrLocalDateTimeDeserializer.class)
    private OffsetDateTime endTime;
    private Integer participantCount;

    private String organizerEmail;
    private String coHostEmail;
    private Boolean registrationRequired;

    private RecurrenceDTO recurrence;


    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getOrganizerEmail() { return organizerEmail; }
    public void setOrganizerEmail(String organizerEmail) { this.organizerEmail = organizerEmail; }
    public String getCoHostEmail() { return coHostEmail; }
    public void setCoHostEmail(String coHostEmail) { this.coHostEmail = coHostEmail; }
    public Boolean getRegistrationRequired() { return registrationRequired; }
    public void setRegistrationRequired(Boolean registrationRequired) { this.registrationRequired = registrationRequired; }
    public OffsetDateTime getStartTime() { return startTime; }
    public void setStartTime(OffsetDateTime startTime) { this.startTime = startTime; }
    public OffsetDateTime getEndTime() { return endTime; }
    public void setEndTime(OffsetDateTime endTime) { this.endTime = endTime; }
    public Integer getParticipantCount() { return participantCount; }
    public void setParticipantCount(Integer participantCount) { this.participantCount = participantCount; }
    public RecurrenceDTO getRecurrence() { return recurrence; }
    public void setRecurrence(RecurrenceDTO recurrence) { this.recurrence = recurrence; }
}
