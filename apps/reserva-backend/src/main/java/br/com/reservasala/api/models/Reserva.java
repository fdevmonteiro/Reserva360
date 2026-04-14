// Reserva.java
package br.com.reservasala.api.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "RESERVAS")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String organizerEmail;
    private boolean needsSlideClicker;
    private boolean createZoomMeeting;
    private String createdByUsername;

    // join_url (você já usa "link")
    private String link;

    // >>> NOVOS CAMPOS
    @Column(name = "start_url", length = 2048)
    private String startUrl;

    @Column(name = "zoom_passcode")
    private String passcode;

    // CUIDADO: campo começa com maiúscula; garanta o nome da coluna
    @Column(name = "zoom_meeting_id")
    private Long ZoomMeetingId;

    private int participantCount;

    @ManyToOne
    private ZoomAccount hostZoomAccount;

    @ManyToOne
    @JoinColumn(name="sala_id", nullable = false)
    private Sala sala;

    @Override
    public String toString() {
        return "Reserva [id=" + id + ", title=" + title + ", description=" + description + ", startTime=" + startTime
                + ", endTime=" + endTime + ", organizerEmail=" + organizerEmail +  ", status="
                + status + ", confirmationToken=" + confirmationToken + "]";
    }

    @Enumerated(EnumType.STRING)
    private ReservationStatus status;

    @Column(unique = true)
    private String confirmationToken;

    public Reserva(){}

    // ===== getters/setters =====

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

    public String getOrganizerEmail() { return organizerEmail; }
    public void setOrganizerEmail(String organizerEmail) { this.organizerEmail = organizerEmail; }

    public String getConfirmationToken(){ return confirmationToken; }
    public void setConfirmationToken(String confirmationToken){ this.confirmationToken = confirmationToken; }

    public ReservationStatus getStatus(){ return status; }
    public void setStatus(ReservationStatus status){ this.status = status; }

    public Sala getSala(){ return sala; }
    public void setSala(Sala sala){ this.sala = sala; }

    public boolean isNeedsSlideClicker() { return needsSlideClicker; }
    public void setNeedsSlideClicker(boolean needsSlideClicker) { this.needsSlideClicker = needsSlideClicker; }

    public boolean isCreateZoomMeeting() { return createZoomMeeting; }
    public void setCreateZoomMeeting(boolean createZoomMeeting) { this.createZoomMeeting = createZoomMeeting; }

    public String getCreatedByUsername() { return createdByUsername; }
    public void setCreatedByUsername(String createdByUsername) { this.createdByUsername = createdByUsername; }

    public Long getZoomMeetingId() { return ZoomMeetingId; }
    public void setZoomMeetingId(Long zoomMeetingId) { ZoomMeetingId = zoomMeetingId; }

    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }

    // >>> getters/setters novos
    public String getStartUrl() { return startUrl; }
    public void setStartUrl(String startUrl) { this.startUrl = startUrl; }

    public String getPasscode() { return passcode; }
    public void setPasscode(String passcode) { this.passcode = passcode; }

    public int getParticipantCount() { return participantCount; }
    public void setParticipantCount(int participantCount) { this.participantCount = participantCount; }

    public ZoomAccount getHostZoomAccount() { return hostZoomAccount; }
    public void setHostZoomAccount(ZoomAccount hostZoomAccount) { this.hostZoomAccount = hostZoomAccount; }
}
