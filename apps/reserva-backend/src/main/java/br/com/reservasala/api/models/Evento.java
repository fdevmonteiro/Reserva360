package br.com.reservasala.api.models;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "eventos")
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- Core ---
    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    private Integer participantCount;

    @Column(nullable = false)
    private String organizerEmail;

    private String coHostEmail;

    @Column(nullable = false)
    private boolean registrationRequired = false;

    // Quem criou via app (username do auth)
    private String createdByUsername;

    // --- Status/Token ---
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status;

    // ATENÇÃO: não pode ser unique porque série usa o mesmo token para todas as ocorrências
    @Column
    private String confirmationToken;

    // --- Zoom ---
    @Column(length = 512)
    private String link; // join_url (p/ participantes)

    @Column(length = 1024)
    private String startUrl; // host start url (não mandar p/ participantes)

    private Long zoomMeetingId;

    // Para reuniões recorrentes: occurrence_id específico da instância no Zoom
    private String zoomOccurrenceId;

    // UUID do meeting no Zoom (necessário para consultar relatórios/past_meetings)
    private String zoomMeetingUuid;

    // Controle para evitar geração duplicada de relatórios
    @Column(nullable = false)
    private boolean reportGenerated = false;

    @OneToOne(mappedBy = "evento", cascade = CascadeType.ALL, orphanRemoval = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private EventoReport report;

    @OneToMany(mappedBy = "evento", cascade = CascadeType.ALL, orphanRemoval = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private java.util.List<InscricaoEvento> inscricoes = new java.util.ArrayList<>();

    // Conta Zoom usada como host (definida na confirmação)
    @ManyToOne
    @JoinColumn(name = "zoom_account_id")
    private ZoomAccount hostZoomAccount;

    // --- Recorrência ---
    // Pai da série (null se este for o pai)
    @ManyToOne
    @JoinColumn(name = "parent_event_id")
    private Evento parentEventId;

    // ===== Getters / Setters =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public Integer getParticipantCount() { return participantCount; }
    public void setParticipantCount(Integer participantCount) { this.participantCount = participantCount; }

    public String getOrganizerEmail() { return organizerEmail; }
    public void setOrganizerEmail(String organizerEmail) { this.organizerEmail = organizerEmail; }

    public String getCoHostEmail() { return coHostEmail; }
    public void setCoHostEmail(String coHostEmail) { this.coHostEmail = coHostEmail; }

    public boolean isRegistrationRequired() { return registrationRequired; }
    public void setRegistrationRequired(boolean registrationRequired) { this.registrationRequired = registrationRequired; }

    public String getCreatedByUsername() { return createdByUsername; }
    public void setCreatedByUsername(String createdByUsername) { this.createdByUsername = createdByUsername; }

    public EventStatus getStatus() { return status; }
    public void setStatus(EventStatus status) { this.status = status; }

    public String getConfirmationToken() { return confirmationToken; }
    public void setConfirmationToken(String confirmationToken) { this.confirmationToken = confirmationToken; }

    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }

    public String getStartUrl() { return startUrl; }
    public void setStartUrl(String startUrl) { this.startUrl = startUrl; }

    public Long getZoomMeetingId() { return zoomMeetingId; }
    public void setZoomMeetingId(Long zoomMeetingId) { this.zoomMeetingId = zoomMeetingId; }

    public String getZoomOccurrenceId() { return zoomOccurrenceId; }
    public void setZoomOccurrenceId(String zoomOccurrenceId) { this.zoomOccurrenceId = zoomOccurrenceId; }

    public String getZoomMeetingUuid() { return zoomMeetingUuid; }
    public void setZoomMeetingUuid(String zoomMeetingUuid) { this.zoomMeetingUuid = zoomMeetingUuid; }

    public boolean isReportGenerated() { return reportGenerated; }
    public void setReportGenerated(boolean reportGenerated) { this.reportGenerated = reportGenerated; }

    public EventoReport getReport() { return report; }
    public void setReport(EventoReport report) { this.report = report; }

    public java.util.List<InscricaoEvento> getInscricoes() { return inscricoes; }
    public void setInscricoes(java.util.List<InscricaoEvento> inscricoes) { this.inscricoes = inscricoes; }

    public ZoomAccount getHostZoomAccount() { return hostZoomAccount; }
    public void setHostZoomAccount(ZoomAccount hostZoomAccount) { this.hostZoomAccount = hostZoomAccount; }

    public Evento getParentEventId() { return parentEventId; }
    public void setParentEventId(Evento parentEventId) { this.parentEventId = parentEventId; }
}
