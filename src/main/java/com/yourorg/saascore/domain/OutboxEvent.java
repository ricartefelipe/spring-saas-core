package com.yourorg.saascore.domain;

import java.time.Instant;
import java.util.UUID;

public class OutboxEvent {

    private final UUID id;
    private final UUID tenantId;
    private final String regionOrigin;
    private final String aggregateType;
    private final String aggregateId;
    private final String type;
    private final String payloadJson;
    private final OutboxStatus status;
    private final int retries;
    private final Instant lockedAt;
    private final String lockedBy;
    private final Instant createdAt;
    private final Instant sentAt;

    public OutboxEvent(
            UUID id,
            UUID tenantId,
            String regionOrigin,
            String aggregateType,
            String aggregateId,
            String type,
            String payloadJson,
            OutboxStatus status,
            int retries,
            Instant lockedAt,
            String lockedBy,
            Instant createdAt,
            Instant sentAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.regionOrigin = regionOrigin;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.type = type;
        this.payloadJson = payloadJson;
        this.status = status;
        this.retries = retries;
        this.lockedAt = lockedAt;
        this.lockedBy = lockedBy;
        this.createdAt = createdAt;
        this.sentAt = sentAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getRegionOrigin() {
        return regionOrigin;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getType() {
        return type;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public int getRetries() {
        return retries;
    }

    public Instant getLockedAt() {
        return lockedAt;
    }

    public String getLockedBy() {
        return lockedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OutboxEvent that)) return false;
        return java.util.Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id);
    }

    public enum OutboxStatus {
        PENDING,
        SENT,
        FAILED
    }
}
