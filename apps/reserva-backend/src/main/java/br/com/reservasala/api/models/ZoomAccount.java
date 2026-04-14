package br.com.reservasala.api.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "zoom_accounts")
public class ZoomAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_email")
    private String accountMail;

    private String zoomUserId;
    private boolean isActive;

    @Column(name = "host_key")
    private String hostkey;

    private boolean isLargeCapacity;

    // ===== Getters / Setters =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAccountMail() { return accountMail; }
    public void setAccountMail(String accountMail) { this.accountMail = accountMail; }

    public String getZoomUserId() { return zoomUserId; }
    public void setZoomUserId(String zoomUserId) { this.zoomUserId = zoomUserId; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean isActive) { this.isActive = isActive; }

    public String getHostkey() { return hostkey; }
    public void setHostkey(String hostkey) { this.hostkey = hostkey; }

    public boolean isLargeCapacity() { return isLargeCapacity; }
    public void setLargeCapacity(boolean isLargeCapacity) { this.isLargeCapacity = isLargeCapacity; }
}
