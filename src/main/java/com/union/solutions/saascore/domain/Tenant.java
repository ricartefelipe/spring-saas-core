package com.union.solutions.saascore.domain;

import java.time.Instant;
import java.util.UUID;

public class Tenant {

    private UUID id;
    private String name;
    private String plan;
    private String region;
    private TenantStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public Tenant() {}

    public Tenant(UUID id, String name, String plan, String region, TenantStatus status,
                  Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.plan = plan;
        this.region = region;
        this.status = status;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPlan() { return plan; }
    public void setPlan(String plan) { this.plan = plan; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public TenantStatus getStatus() { return status; }
    public void setStatus(TenantStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public boolean isActive() {
        return status == TenantStatus.ACTIVE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tenant that)) return false;
        return java.util.Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id);
    }

    public enum TenantStatus {
        ACTIVE, SUSPENDED, DELETED
    }
}
