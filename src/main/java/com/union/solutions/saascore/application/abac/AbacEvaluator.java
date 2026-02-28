package com.union.solutions.saascore.application.abac;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.union.solutions.saascore.application.port.PolicyRepository;
import com.union.solutions.saascore.config.TenantContext;
import com.union.solutions.saascore.domain.Policy;
import io.micrometer.core.instrument.Counter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AbacEvaluator {

  private static final Logger log = LoggerFactory.getLogger(AbacEvaluator.class);

  private final PolicyRepository policyRepo;
  private final AuditLogger auditLogger;
  private final ObjectMapper objectMapper;
  private final Counter accessDeniedCounter;

  public AbacEvaluator(
      PolicyRepository policyRepo,
      AuditLogger auditLogger,
      ObjectMapper objectMapper,
      @Qualifier("accessDeniedCounter") Counter accessDeniedCounter) {
    this.policyRepo = policyRepo;
    this.auditLogger = auditLogger;
    this.objectMapper = objectMapper;
    this.accessDeniedCounter = accessDeniedCounter;
  }

  @Transactional(readOnly = true)
  public AbacResult evaluate(AbacContext ctx) {
    List<Policy> policies = policyRepo.findByPermissionCodeAndEnabledTrue(ctx.permission());
    if (policies.isEmpty()) {
      logDeny(ctx, null);
      return AbacResult.deny(null, "no_matching_allow_policy");
    }
    for (Policy p : policies) {
      if (p.getEffect() == Policy.Effect.DENY && p.appliesTo(ctx.plan(), ctx.region())) {
        logDeny(ctx, p);
        return AbacResult.deny(p.getId(), "denied_by_policy");
      }
    }
    boolean hasAllow = false;
    for (Policy p : policies) {
      if (p.getEffect() == Policy.Effect.ALLOW && p.appliesTo(ctx.plan(), ctx.region())) {
        hasAllow = true;
        break;
      }
    }
    if (!hasAllow) {
      logDeny(ctx, null);
      return AbacResult.deny(null, "no_matching_allow_policy");
    }
    return AbacResult.allow();
  }

  private void logDeny(AbacContext ctx, Policy policy) {
    accessDeniedCounter.increment();
    String policyId = policy != null ? policy.getId().toString() : "none";
    log.warn(
        "ACCESS_DENIED tenant={} sub={} perm={} plan={} region={} policy={} corr={}",
        ctx.tenantId(),
        ctx.subject(),
        ctx.permission(),
        ctx.plan(),
        ctx.region(),
        policyId,
        ctx.correlationId());
    try {
      UUID tenantId = ctx.tenantId();
      String details =
          objectMapper.writeValueAsString(
              Map.of(
                  "permission", ctx.permission() != null ? ctx.permission() : "",
                  "plan", ctx.plan() != null ? ctx.plan() : "",
                  "region", ctx.region() != null ? ctx.region() : "",
                  "policy_id", policyId,
                  "reason", policy != null ? "denied_by_policy" : "no_matching_allow_policy"));
      auditLogger.log(
          tenantId,
          ctx.subject(),
          TenantContext.getRoles().toString(),
          TenantContext.getPerms().toString(),
          "ACCESS_DENIED",
          "permission",
          ctx.permission(),
          null,
          null,
          403,
          ctx.correlationId(),
          details);
    } catch (Exception e) {
      log.error("Failed to write audit log for deny", e);
    }
  }
}
