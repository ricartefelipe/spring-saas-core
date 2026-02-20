package com.yourorg.saascore.unit.application.abac;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yourorg.saascore.application.abac.AbacContext;
import com.yourorg.saascore.application.abac.AbacEvaluator;
import com.yourorg.saascore.application.abac.AbacResult;
import com.yourorg.saascore.application.abac.AuditLogger;
import com.yourorg.saascore.adapters.out.persistence.AbacPolicyEntity;
import com.yourorg.saascore.adapters.out.persistence.AbacPolicyJpaRepository;
import com.yourorg.saascore.domain.AbacPolicy;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AbacEvaluatorTest {

    @Mock
    AbacPolicyJpaRepository policyRepo;

    @Mock
    AuditLogger auditLogger;

    @Test
    void evaluate_whenNoPolicies_returnsAllow() {
        var objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var evaluator = new AbacEvaluator(policyRepo, auditLogger, objectMapper);
        when(policyRepo.findByTenantIdAndEnabledTrueOrderByPriorityDesc(any())).thenReturn(List.of());

        AbacContext ctx =
                new AbacContext(
                        UUID.randomUUID(),
                        "user@example.com",
                        "jti-1",
                        "admin:write",
                        "free",
                        "region-a",
                        null,
                        "corr-1",
                        null,
                        System.currentTimeMillis());
        AbacResult result = evaluator.evaluate(ctx);

        assertThat(result.allowed()).isTrue();
    }

    @Test
    void evaluate_whenDenyPolicyMatchesPlan_returnsDenyAndLogs() {
        var objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var evaluator = new AbacEvaluator(policyRepo, auditLogger, objectMapper);
        UUID tenantId = UUID.randomUUID();
        UUID policyId = UUID.randomUUID();
        AbacPolicyEntity policy = new AbacPolicyEntity();
        policy.setId(policyId);
        policy.setTenantId(tenantId);
        policy.setPermissionCode("admin:write");
        policy.setEffect(AbacPolicy.Effect.DENY);
        policy.setPriority(10);
        policy.setEnabled(true);
        policy.setConditionsJson("{\"plan\":\"free\"}");
        when(policyRepo.findByTenantIdAndEnabledTrueOrderByPriorityDesc(tenantId))
                .thenReturn(List.of(policy));

        AbacContext ctx =
                new AbacContext(
                        tenantId,
                        "user@example.com",
                        "jti-1",
                        "admin:write",
                        "free",
                        "region-a",
                        null,
                        "corr-1",
                        null,
                        System.currentTimeMillis());
        AbacResult result = evaluator.evaluate(ctx);

        assertThat(result.allowed()).isFalse();
        assertThat(result.policyId()).isEqualTo(policyId);
        assertThat(result.reason()).isEqualTo("policy_condition_matched");
        verify(auditLogger)
                .log(
                        eq(tenantId),
                        eq("user@example.com"),
                        eq("jti-1"),
                        eq("abac.evaluate"),
                        eq("admin:write"),
                        eq("DENY"),
                        eq("policy_condition_matched"),
                        eq(policyId),
                        eq("corr-1"),
                        any(),
                        any());
    }
}
