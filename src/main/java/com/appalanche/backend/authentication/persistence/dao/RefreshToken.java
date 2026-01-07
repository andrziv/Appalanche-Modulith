package com.appalanche.backend.authentication.persistence.dao;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

import static jakarta.persistence.FetchType.LAZY;

@Table(name = "refresh_tokens")
@Entity
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(nullable = false, updatable = false, name = "account_id")
    private Account account;

    @Column(unique = true, nullable = false, name = "token_id")
    private UUID tokenId;

    @Column(nullable = false)
    private String token;

    @Column(name = "device_name")
    private String deviceName;

    @Column(nullable = false, name = "expiry_date")
    private Instant expiryDate;

    @Column(name = "last_used")
    private Instant lastUsed;

    public RefreshToken() {

    }

    public RefreshToken(Account account, UUID tokenId, String token, String deviceName, Instant expiryDate, Instant lastUsed) {
        this.account = account;
        this.tokenId = tokenId;
        this.token = token;
        this.deviceName = deviceName;
        this.expiryDate = expiryDate;
        this.lastUsed = lastUsed;
    }

    public Long getId() {
        return id;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public UUID getTokenId() {
        return tokenId;
    }

    public void setTokenId(UUID tokenId) {
        this.tokenId = tokenId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public Instant getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Instant expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Instant getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(Instant lastUsed) {
        this.lastUsed = lastUsed;
    }
}
