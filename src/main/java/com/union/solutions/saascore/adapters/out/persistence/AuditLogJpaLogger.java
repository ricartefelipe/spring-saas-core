package com.union.solutions.saascore.adapters.out.persistence;

import com.union.solutions.saascore.application.abac.AuditLogger;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AuditLogJpaLogger implements AuditLogger {

  private final AuditLogJpaRepository repo;

  public AuditLogJpaLogger(AuditLogJpaRepository repo) {
    this.repo = repo;
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void log(
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
      String details) {
    AuditLogEntity e = new AuditLogEntity();
    e.setTenantId(tenantId);
    e.setActorSub(actorSub);
    e.setActorRoles(actorRoles);
    e.setActorPerms(actorPerms);
    e.setAction(action);
    e.setResourceType(resourceType);
    e.setResourceId(resourceId);
    e.setMethod(method);
    e.setPath(path);
    e.setStatusCode(statusCode);
    e.setCorrelationId(correlationId);
    e.setDetails(details);
    e.setCreatedAt(Instant.now());
    repo.save(e);
  }
}
