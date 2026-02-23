package com.union.solutions.saascore.application.abac;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.union.solutions.saascore.adapters.out.persistence.PolicyEntity;
import com.union.solutions.saascore.adapters.out.persistence.PolicyJpaRepository;
import com.union.solutions.saascore.config.TenantContext;
import com.union.solutions.saascore.domain.Policy;
import io.micrometer.core.instrument.Counter;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AbacEvaluator {

    private static final Logger log = LoggerFactory.getLogger(AbacEvaluator.class);
    private static final TypeReference<List<String>> LIST_TYPE = new TypeReference<>() {};

    private final PolicyJpaRepository policyRepo;
    private final AuditLogger auditLogger;
    private final ObjectMapper objectMapper;
    private final Counter accessDeniedCounter;

    public AbacEvaluator(PolicyJpaRepository policyRepo, AuditLogger auditLogger,
                         ObjectMapper objectMapper,
                         @Qualifier("accessDeniedCounter") Counter accessDeniedCounter) {
        this.policyRepo = policyRepo;
        this.auditLogger = auditLogger;
        this.objectMapper = objectMapper;
        this.accessDeniedCounter = accessDeniedCounter;
    }

    @Transactional(readOnly = true)
    public AbacResult evaluate(AbacContext ctx) {
        List<PolicyEntity> policies = policyRepo.findByPermissionCodeAndEnabledTrue(ctx.permission());
        if (policies.isEmpty()) {
            return AbacResult.allow();
        }
        for (PolicyEntity pe : policies) {
            if (pe.getEffect() == Policy.Effect.DENY && matchesContext(pe, ctx)) {
                logDeny(ctx, pe);
                return AbacResult.deny(pe.getId(), "denied_by_policy");
            }
        }
        boolean hasAllow = false;
        for (PolicyEntity pe : policies) {
            if (pe.getEffect() == Policy.Effect.ALLOW && matchesContext(pe, ctx)) {
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

    private boolean matchesContext(PolicyEntity pe, AbacContext ctx) {
        List<String> plans = parseJson(pe.getAllowedPlans());
        List<String> regions = parseJson(pe.getAllowedRegions());
        boolean planMatch = plans.isEmpty() || plans.contains(ctx.plan());
        boolean regionMatch = regions.isEmpty() || regions.contains(ctx.region());
        return planMatch && regionMatch;
    }

    private List<String> parseJson(String json) {
        if (json == null || json.isBlank() || "[]".equals(json)) return List.of();
        try {
            return objectMapper.readValue(json, LIST_TYPE);
        } catch (Exception e) {
            log.warn("Failed to parse policy JSON: {}", e.getMessage());
            return List.of();
        }
    }

    private void logDeny(AbacContext ctx, PolicyEntity policy) {
        accessDeniedCounter.increment();
        String policyId = policy != null ? policy.getId().toString() : "none";
        log.warn("ACCESS_DENIED tenant={} sub={} perm={} plan={} region={} policy={} corr={}",
                ctx.tenantId(), ctx.subject(), ctx.permission(), ctx.plan(), ctx.region(),
                policyId, ctx.correlationId());
        try {
            UUID tenantId = ctx.tenantId();
            String details = objectMapper.writeValueAsString(java.util.Map.of(
                    "permission", ctx.permission() != null ? ctx.permission() : "",
                    "plan", ctx.plan() != null ? ctx.plan() : "",
                    "region", ctx.region() != null ? ctx.region() : "",
                    "policy_id", policyId,
                    "reason", policy != null ? "denied_by_policy" : "no_matching_allow_policy"));
            auditLogger.log(tenantId, ctx.subject(),
                    TenantContext.getRoles().toString(), TenantContext.getPerms().toString(),
                    "ACCESS_DENIED", "permission", ctx.permission(),
                    null, null, 403, ctx.correlationId(), details);
        } catch (Exception e) {
            log.error("Failed to write audit log for deny", e);
        }
    }
}
