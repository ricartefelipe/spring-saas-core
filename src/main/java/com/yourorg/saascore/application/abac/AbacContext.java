package com.yourorg.saascore.application.abac;

import java.util.UUID;

public record AbacContext(
        UUID tenantId,
        String subject,
        String jti,
        String permission,
        String plan,
        String region,
        String ip,
        String correlationId,
        String traceId,
        long evaluationTimeMillis) {

    public static AbacContext fromCurrentContext(String permission) {
        return new AbacContext(
                com.yourorg.saascore.config.TenantContext.getTenantId().orElse(null),
                com.yourorg.saascore.config.TenantContext.getSubject(),
                com.yourorg.saascore.config.TenantContext.getJti(),
                permission,
                com.yourorg.saascore.config.TenantContext.getPlan(),
                com.yourorg.saascore.config.TenantContext.getRegion(),
                null,
                com.yourorg.saascore.config.TenantContext.getCorrelationId(),
                null,
                System.currentTimeMillis());
    }
}
