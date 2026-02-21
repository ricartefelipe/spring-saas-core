package com.yourorg.saascore.domain;

import java.time.Instant;
import java.util.UUID;

public class FeatureFlag {

    private final UUID id;
    private final UUID tenantId;
    private final String name;
    private final boolean enabled;
    private final int rolloutPercentage;
    private final String targetingJson;
    private final Instant updatedAt;

    public FeatureFlag(
            UUID id,
            UUID tenantId,
            String name,
            boolean enabled,
            int rolloutPercentage,
            String targetingJson,
            Instant updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.name = name;
        this.enabled = enabled;
        this.rolloutPercentage = Math.max(0, Math.min(100, rolloutPercentage));
        this.targetingJson = targetingJson;
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getRolloutPercentage() {
        return rolloutPercentage;
    }

    public String getTargetingJson() {
        return targetingJson;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FeatureFlag that)) return false;
        return java.util.Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id);
    }
}
