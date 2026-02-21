package com.yourorg.saascore.application.abac;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourorg.saascore.config.TenantContext;
import com.yourorg.saascore.domain.AbacPolicy;
import com.yourorg.saascore.adapters.out.persistence.AbacPolicyEntity;
import com.yourorg.saascore.adapters.out.persistence.AbacPolicyJpaRepository;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AbacEvaluator {

    private static final Logger log = LoggerFactory.getLogger(AbacEvaluator.class);

    private final AbacPolicyJpaRepository policyRepo;
    private final AuditLogger auditLogger;
    private final ObjectMapper objectMapper;

    public AbacEvaluator(
            AbacPolicyJpaRepository policyRepo,
            AuditLogger auditLogger,
            ObjectMapper objectMapper) {
        this.policyRepo = policyRepo;
        this.auditLogger = auditLogger;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AbacResult evaluate(AbacContext ctx) {
        if (ctx.tenantId() == null) {
            return AbacResult.allow();
        }
        List<AbacPolicyEntity> policies =
                policyRepo.findByTenantIdAndEnabledTrueOrderByPriorityDesc(ctx.tenantId());
        for (AbacPolicyEntity pe : policies) {
            if (!matchesPermission(pe, ctx.permission())) continue;
            boolean conditionsMatch = evaluateConditions(pe.getConditionsJson(), ctx);
            if (conditionsMatch && pe.getEffect() == AbacPolicy.Effect.DENY) {
                logAbacDecision(ctx, pe, "DENY");
                auditLogger.log(
                        ctx.tenantId(),
                        ctx.subject(),
                        ctx.jti(),
                        "abac.evaluate",
                        ctx.permission(),
                        "DENY",
                        "policy_condition_matched",
                        pe.getId(),
                        ctx.correlationId(),
                        ctx.traceId(),
                        null);
                return AbacResult.deny(pe.getId(), "policy_condition_matched");
            }
        }
        logAbacDecision(ctx, null, "ALLOW");
        return AbacResult.allow();
    }

    private boolean matchesPermission(AbacPolicyEntity pe, String permission) {
        if (permission == null) return false;
        String code = pe.getPermissionCode();
        return permission.equals(code) || "*".equals(code);
    }

    private boolean evaluateConditions(String conditionsJson, AbacContext ctx) {
        if (conditionsJson == null || conditionsJson.isBlank()) return true;
        try {
            JsonNode node = objectMapper.readTree(conditionsJson);
            if (node.has("plan")) {
                String required = node.get("plan").asText();
                if (ctx.plan() != null && !required.equals(ctx.plan())) return false;
            }
            if (node.has("region")) {
                String required = node.get("region").asText();
                if (ctx.region() != null && !required.equals(ctx.region())) return false;
            }
            if (node.has("ip_allowlist")) {
                JsonNode list = node.get("ip_allowlist");
                if (list.isArray() && ctx.ip() != null) {
                    boolean allowed = false;
                    for (JsonNode allowedIp : list) {
                        if (ctx.ip().equals(allowedIp.asText())) {
                            allowed = true;
                            break;
                        }
                    }
                    if (!allowed) return false;
                }
            }
            if (node.has("time_window")) {
                JsonNode tw = node.get("time_window");
                if (tw.has("start_hour") && tw.has("end_hour")) {
                    int start = tw.get("start_hour").asInt();
                    int end = tw.get("end_hour").asInt();
                    int hour = java.time.ZonedDateTime.now().getHour();
                    if (start <= end) {
                        if (hour < start || hour > end) return false;
                    } else {
                        if (hour < start && hour > end) return false;
                    }
                }
            }
            return true;
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.warn("ABAC conditions parse error: {}", e.getMessage());
            return false;
        }
    }

    private void logAbacDecision(AbacContext ctx, AbacPolicyEntity policy, String decision) {
        log.info(
                "ABAC decision={} tenant_id={} subject={} permission={} policy_id={} correlation_id={}",
                decision,
                ctx.tenantId(),
                ctx.subject(),
                ctx.permission(),
                policy != null ? policy.getId() : null,
                ctx.correlationId());
    }
}
