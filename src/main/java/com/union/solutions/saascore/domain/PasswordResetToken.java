package com.union.solutions.saascore.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class PasswordResetToken {

    private UUID id;
    private UUID userId;
    private String tokenHash;
    private boolean used;
    private Instant expiresAt;
    private Instant createdAt;

    public PasswordResetToken() {}

    public PasswordResetToken(UUID id, UUID userId, String tokenHash, boolean used,
                              Instant expiresAt, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.used = used;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public boolean isExpired() { return Instant.now().isAfter(expiresAt); }
    public boolean isValid() { return !used && !isExpired(); }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PasswordResetToken that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
