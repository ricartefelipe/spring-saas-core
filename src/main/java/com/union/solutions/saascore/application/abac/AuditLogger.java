package com.union.solutions.saascore.application.abac;

import java.util.UUID;

public interface AuditLogger {

  void log(
      UUID tenantId,
      String actorSub,
      String actorRoles,
      String actorPerms,
      String action,
      String resourceType,
      String resourceId,
      String method,
      String path,
      Integer statusCode,
      String correlationId,
      String details);
}
