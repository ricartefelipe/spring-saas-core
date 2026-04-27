package com.union.solutions.saascore.adapters.out.persistence;

import com.union.solutions.saascore.application.port.AuditLogger;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AuditLogJpaLogger implements AuditLogger {

  private static final Logger log = LoggerFactory.getLogger(AuditLogJpaLogger.class);

  /**
   * Align with {@code audit_log} Liquibase / PostgreSQL column sizes. Values longer than the limit
   * would cause {@link org.springframework.dao.DataIntegrityViolationException} and roll back the
   * outer business transaction (e.g. user soft delete).
   */
  private static final int MAX_ACTOR_SUB = 255;

  private static final int MAX_ACTION = 64;
  private static final int MAX_RESOURCE_TYPE = 64;
  private static final int MAX_RESOURCE_ID = 255;
  private static final int MAX_METHOD = 10;
  private static final int MAX_PATH = 512;
  private static final int MAX_CORRELATION_ID = 64;

  private final AuditLogJpaRepository repo;

  public AuditLogJpaLogger(AuditLogJpaRepository repo) {
    this.repo = repo;
  }

  private static String trunc(String s, int max) {
    if (s == null) {
      return null;
    }
    if (s.length() <= max) {
      return s;
    }
    return s.substring(0, max);
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
    if (actorSub != null && actorSub.length() > MAX_ACTOR_SUB) {
      log.debug(
          "Audit actor_sub truncated to {} (was {} chars, e.g. OIDC sub)",
          MAX_ACTOR_SUB,
          actorSub.length());
    }
    AuditLogEntity e = new AuditLogEntity();
    e.setTenantId(tenantId);
    e.setActorSub(trunc(actorSub, MAX_ACTOR_SUB));
    e.setActorRoles(actorRoles);
    e.setActorPerms(actorPerms);
    e.setAction(trunc(action, MAX_ACTION));
    e.setResourceType(trunc(resourceType, MAX_RESOURCE_TYPE));
    e.setResourceId(trunc(resourceId, MAX_RESOURCE_ID));
    e.setMethod(trunc(method, MAX_METHOD));
    e.setPath(trunc(path, MAX_PATH));
    e.setStatusCode(statusCode);
    e.setCorrelationId(trunc(correlationId, MAX_CORRELATION_ID));
    e.setDetails(details);
    e.setCreatedAt(Instant.now());
    repo.save(e);
  }
}
