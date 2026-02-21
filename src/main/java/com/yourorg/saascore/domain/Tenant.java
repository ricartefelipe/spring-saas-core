package com.yourorg.saascore.domain;

import java.time.Instant;
import java.util.UUID;

public class Tenant {

    private final UUID id;
    private final String name;
    private final TenantStatus status;
    private final Plan plan;
    private final String primaryRegion;
    private final String shardKey;
    private final Instant createdAt;

    public Tenant(
            UUID id,
            String name,
            TenantStatus status,
            Plan plan,
            String primaryRegion,
            String shardKey,
            Instant createdAt) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.plan = plan;
        this.primaryRegion = primaryRegion;
        this.shardKey = shardKey;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public TenantStatus getStatus() {
        return status;
    }

    public Plan getPlan() {
        return plan;
    }

    public String getPrimaryRegion() {
        return primaryRegion;
    }

    public String getShardKey() {
        return shardKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

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
        ACTIVE,
        SUSPENDED,
        DELETED
    }

    public enum Plan {
        free,
        pro,
        enterprise
    }
}
