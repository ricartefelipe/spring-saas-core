package com.union.solutions.saascore.application.abac;

import com.union.solutions.saascore.config.TenantContext;
import java.util.UUID;

public record AbacContext(
    UUID tenantId,
    String subject,
    String permission,
    String plan,
    String region,
    String correlationId) {

  public static AbacContext fromCurrentContext(String permission) {
    return new AbacContext(
        TenantContext.getTenantId().orElse(null),
        TenantContext.getSubject(),
        permission,
        TenantContext.getPlan(),
        TenantContext.getRegion(),
        TenantContext.getCorrelationId());
  }
}
