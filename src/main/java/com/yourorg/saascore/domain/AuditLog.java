package com.yourorg.saascore.domain;

import java.time.Instant;
import java.util.UUID;

public class AuditLog {

    private final UUID id;
    private final UUID tenantId;
    private final String subject;
    private final String jti;
    private final String action;
    private final String resource;
    private final String decision;
    private final String reason;
    private final UUID policyId;
    private final String correlationId;
    private final String traceId;
    private final Instant createdAt;
    private final String metadataJson;

    public AuditLog(
            UUID id,
            UUID tenantId,
            String subject,
            String jti,
            String action,
            String resource,
            String decision,
            String reason,
            UUID policyId,
            String correlationId,
            String traceId,
            Instant createdAt,
            String metadataJson) {
        this.id = id;
        this.tenantId = tenantId;
        this.subject = subject;
        this.jti = jti;
        this.action = action;
        this.resource = resource;
        this.decision = decision;
        this.reason = reason;
        this.policyId = policyId;
        this.correlationId = correlationId;
        this.traceId = traceId;
        this.createdAt = createdAt;
        this.metadataJson = metadataJson;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getSubject() {
        return subject;
    }

    public String getJti() {
        return jti;
    }

    public String getAction() {
        return action;
    }

    public String getResource() {
        return resource;
    }

    public String getDecision() {
        return decision;
    }

    public String getReason() {
        return reason;
    }

    public UUID getPolicyId() {
        return policyId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getTraceId() {
        return traceId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getMetadataJson() {
        return metadataJson;
    }
}
