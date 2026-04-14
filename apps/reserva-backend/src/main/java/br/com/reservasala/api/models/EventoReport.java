package br.com.reservasala.api.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "evento_reports")
public class EventoReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "evento_id", unique = true, nullable = false)
    @JsonIgnore
    private Evento evento;

    private String meetingUuid;
    private String topic;
    private LocalDateTime actualStartTime;
    private LocalDateTime actualEndTime;
    private Integer durationMinutes;
    private Integer totalParticipants;
    private Integer peakParticipants;
    private Integer totalMinutes;
    private String recordingUrl;
    private String recordingStatus;
    private LocalDateTime generatedAt;

    private boolean emailed = false;
    private LocalDateTime emailedAt;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventoReportParticipant> participants = new ArrayList<>();

    // ===== Getters / Setters =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Evento getEvento() { return evento; }
    public void setEvento(Evento evento) { this.evento = evento; }

    public String getMeetingUuid() { return meetingUuid; }
    public void setMeetingUuid(String meetingUuid) { this.meetingUuid = meetingUuid; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public LocalDateTime getActualStartTime() { return actualStartTime; }
    public void setActualStartTime(LocalDateTime actualStartTime) { this.actualStartTime = actualStartTime; }

    public LocalDateTime getActualEndTime() { return actualEndTime; }
    public void setActualEndTime(LocalDateTime actualEndTime) { this.actualEndTime = actualEndTime; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public Integer getTotalParticipants() { return totalParticipants; }
    public void setTotalParticipants(Integer totalParticipants) { this.totalParticipants = totalParticipants; }

    public Integer getPeakParticipants() { return peakParticipants; }
    public void setPeakParticipants(Integer peakParticipants) { this.peakParticipants = peakParticipants; }

    public Integer getTotalMinutes() { return totalMinutes; }
    public void setTotalMinutes(Integer totalMinutes) { this.totalMinutes = totalMinutes; }

    public String getRecordingUrl() { return recordingUrl; }
    public void setRecordingUrl(String recordingUrl) { this.recordingUrl = recordingUrl; }

    public String getRecordingStatus() { return recordingStatus; }
    public void setRecordingStatus(String recordingStatus) { this.recordingStatus = recordingStatus; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

    public boolean isEmailed() { return emailed; }
    public void setEmailed(boolean emailed) { this.emailed = emailed; }

    public LocalDateTime getEmailedAt() { return emailedAt; }
    public void setEmailedAt(LocalDateTime emailedAt) { this.emailedAt = emailedAt; }

    public List<EventoReportParticipant> getParticipants() { return participants; }
    public void setParticipants(List<EventoReportParticipant> participants) { this.participants = participants; }
}
