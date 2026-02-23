package com.union.solutions.saascore.unit.application.abac;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.union.solutions.saascore.adapters.out.persistence.PolicyEntity;
import com.union.solutions.saascore.adapters.out.persistence.PolicyJpaRepository;
import com.union.solutions.saascore.application.abac.AbacContext;
import com.union.solutions.saascore.application.abac.AbacEvaluator;
import com.union.solutions.saascore.application.abac.AbacResult;
import com.union.solutions.saascore.application.abac.AuditLogger;
import com.union.solutions.saascore.domain.Policy;
import io.micrometer.core.instrument.Counter;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AbacEvaluatorTest {

    @Mock PolicyJpaRepository policyRepo;
    @Mock AuditLogger auditLogger;
    @Mock Counter accessDeniedCounter;

    private AbacEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new AbacEvaluator(policyRepo, auditLogger, new ObjectMapper(), accessDeniedCounter);
    }

    @Test
    void evaluate_noPolicies_returnsAllow() {
        when(policyRepo.findByPermissionCodeAndEnabledTrue("admin:write")).thenReturn(List.of());
        AbacContext ctx = new AbacContext(UUID.randomUUID(), "user@test", "admin:write", "pro", "us-east-1", "corr-1");
        AbacResult result = evaluator.evaluate(ctx);
        assertThat(result.allowed()).isTrue();
    }

    @Test
    void evaluate_denyPolicyMatchesPlan_returnsDeny() {
        UUID policyId = UUID.randomUUID();
        PolicyEntity deny = makePolicyEntity(policyId, "admin:write", Policy.Effect.DENY, "[\"free\"]", "[]");
        when(policyRepo.findByPermissionCodeAndEnabledTrue("admin:write")).thenReturn(List.of(deny));

        AbacContext ctx = new AbacContext(UUID.randomUUID(), "user@test", "admin:write", "free", "us-east-1", "corr-1");
        AbacResult result = evaluator.evaluate(ctx);

        assertThat(result.allowed()).isFalse();
        assertThat(result.policyId()).isEqualTo(policyId);
        assertThat(result.reason()).isEqualTo("denied_by_policy");
        verify(accessDeniedCounter).increment();
    }

    @Test
    void evaluate_denyPolicyDoesNotMatchPlan_returnsAllow() {
        PolicyEntity deny = makePolicyEntity(UUID.randomUUID(), "admin:write", Policy.Effect.DENY, "[\"free\"]", "[]");
        PolicyEntity allow = makePolicyEntity(UUID.randomUUID(), "admin:write", Policy.Effect.ALLOW, "[]", "[]");
        when(policyRepo.findByPermissionCodeAndEnabledTrue("admin:write")).thenReturn(List.of(deny, allow));

        AbacContext ctx = new AbacContext(UUID.randomUUID(), "user@test", "admin:write", "enterprise", "us-east-1", "corr-1");
        AbacResult result = evaluator.evaluate(ctx);

        assertThat(result.allowed()).isTrue();
    }

    @Test
    void evaluate_denyTakesPrecedenceOverAllow() {
        UUID denyId = UUID.randomUUID();
        PolicyEntity deny = makePolicyEntity(denyId, "admin:write", Policy.Effect.DENY, "[]", "[]");
        PolicyEntity allow = makePolicyEntity(UUID.randomUUID(), "admin:write", Policy.Effect.ALLOW, "[]", "[]");
        when(policyRepo.findByPermissionCodeAndEnabledTrue("admin:write")).thenReturn(List.of(deny, allow));

        AbacContext ctx = new AbacContext(UUID.randomUUID(), "user@test", "admin:write", "free", "us-east-1", "corr-1");
        AbacResult result = evaluator.evaluate(ctx);

        assertThat(result.allowed()).isFalse();
        assertThat(result.policyId()).isEqualTo(denyId);
    }

    @Test
    void evaluate_noMatchingAllowPolicy_returnsDeny() {
        PolicyEntity allow = makePolicyEntity(UUID.randomUUID(), "admin:write", Policy.Effect.ALLOW, "[\"enterprise\"]", "[]");
        when(policyRepo.findByPermissionCodeAndEnabledTrue("admin:write")).thenReturn(List.of(allow));

        AbacContext ctx = new AbacContext(UUID.randomUUID(), "user@test", "admin:write", "free", "us-east-1", "corr-1");
        AbacResult result = evaluator.evaluate(ctx);

        assertThat(result.allowed()).isFalse();
        assertThat(result.reason()).isEqualTo("no_matching_allow_policy");
    }

    @Test
    void evaluate_regionFilter_worksCorrectly() {
        PolicyEntity allow = makePolicyEntity(UUID.randomUUID(), "admin:write", Policy.Effect.ALLOW, "[]", "[\"eu-west-1\"]");
        when(policyRepo.findByPermissionCodeAndEnabledTrue("admin:write")).thenReturn(List.of(allow));

        AbacContext euCtx = new AbacContext(UUID.randomUUID(), "user@test", "admin:write", "pro", "eu-west-1", "corr-1");
        assertThat(evaluator.evaluate(euCtx).allowed()).isTrue();

        AbacContext usCtx = new AbacContext(UUID.randomUUID(), "user@test", "admin:write", "pro", "us-east-1", "corr-2");
        assertThat(evaluator.evaluate(usCtx).allowed()).isFalse();
    }

    private PolicyEntity makePolicyEntity(UUID id, String permCode, Policy.Effect effect,
                                           String allowedPlans, String allowedRegions) {
        PolicyEntity e = new PolicyEntity();
        e.setId(id);
        e.setPermissionCode(permCode);
        e.setEffect(effect);
        e.setAllowedPlans(allowedPlans);
        e.setAllowedRegions(allowedRegions);
        e.setEnabled(true);
        Instant now = Instant.now();
        e.setCreatedAt(now);
        e.setUpdatedAt(now);
        return e;
    }
}
