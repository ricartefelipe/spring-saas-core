package com.yourorg.saascore.application.abac;

import java.util.UUID;

public interface AuditLogger {

    void log(
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
            String metadataJson);
}
