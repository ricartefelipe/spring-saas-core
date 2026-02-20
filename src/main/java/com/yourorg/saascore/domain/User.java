package com.yourorg.saascore.domain;

import java.time.Instant;
import java.util.UUID;

public class User {

    private final UUID id;
    private final UUID tenantId;
    private final String email;
    private final String passwordHash;
    private final UserStatus status;
    private final Instant createdAt;

    public User(
            UUID id,
            UUID tenantId,
            String email,
            String passwordHash,
            UserStatus status,
            Instant createdAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.status = status;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public enum UserStatus {
        ACTIVE,
        SUSPENDED,
        DELETED
    }
}
