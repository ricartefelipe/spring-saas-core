package com.union.solutions.saascore.unit.application.abac;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.union.solutions.saascore.application.abac.AbacContext;
import com.union.solutions.saascore.application.abac.AbacEvaluator;
import com.union.solutions.saascore.application.abac.AbacResult;
import com.union.solutions.saascore.application.abac.AuditLogger;
import com.union.solutions.saascore.application.port.PolicyRepository;
import com.union.solutions.saascore.domain.Policy;
import io.micrometer.core.instrument.Counter;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AbacEvaluatorTest {

  @Mock PolicyRepository policyRepo;
  @Mock AuditLogger auditLogger;
  @Mock Counter accessDeniedCounter;

  private AbacEvaluator evaluator;

  @BeforeEach
  void setUp() {
    evaluator = new AbacEvaluator(policyRepo, auditLogger, new ObjectMapper(), accessDeniedCounter);
  }

  @Test
  void evaluate_noPolicies_returnsDeny_defaultDeny() {
    when(policyRepo.findByPermissionCodeAndEnabledTrue("admin:write")).thenReturn(List.of());
    AbacContext ctx =
        new AbacContext(
            UUID.randomUUID(), "user@test", "admin:write", "pro", "us-east-1", "corr-1");
    AbacResult result = evaluator.evaluate(ctx);
    assertThat(result.allowed()).isFalse();
    assertThat(result.reason()).isEqualTo("no_matching_allow_policy");
    verify(accessDeniedCounter).increment();
  }

  @Test
  void evaluate_denyPolicyMatchesPlan_returnsDeny() {
    UUID policyId = UUID.randomUUID();
    Policy deny = makePolicy(policyId, "admin:write", Policy.Effect.DENY, List.of("free"), List.of());
    when(policyRepo.findByPermissionCodeAndEnabledTrue("admin:write")).thenReturn(List.of(deny));

    AbacContext ctx =
        new AbacContext(
            UUID.randomUUID(), "user@test", "admin:write", "free", "us-east-1", "corr-1");
    AbacResult result = evaluator.evaluate(ctx);

    assertThat(result.allowed()).isFalse();
    assertThat(result.policyId()).isEqualTo(policyId);
    assertThat(result.reason()).isEqualTo("denied_by_policy");
    verify(accessDeniedCounter).increment();
  }

  @Test
  void evaluate_denyPolicyDoesNotMatchPlan_returnsAllow() {
    Policy deny = makePolicy(UUID.randomUUID(), "admin:write", Policy.Effect.DENY, List.of("free"), List.of());
    Policy allow = makePolicy(UUID.randomUUID(), "admin:write", Policy.Effect.ALLOW, List.of(), List.of());
    when(policyRepo.findByPermissionCodeAndEnabledTrue("admin:write"))
        .thenReturn(List.of(deny, allow));

    AbacContext ctx =
        new AbacContext(
            UUID.randomUUID(), "user@test", "admin:write", "enterprise", "us-east-1", "corr-1");
    AbacResult result = evaluator.evaluate(ctx);

    assertThat(result.allowed()).isTrue();
  }

  @Test
  void evaluate_denyTakesPrecedenceOverAllow() {
    UUID denyId = UUID.randomUUID();
    Policy deny = makePolicy(denyId, "admin:write", Policy.Effect.DENY, List.of(), List.of());
    Policy allow = makePolicy(UUID.randomUUID(), "admin:write", Policy.Effect.ALLOW, List.of(), List.of());
    when(policyRepo.findByPermissionCodeAndEnabledTrue("admin:write"))
        .thenReturn(List.of(deny, allow));

    AbacContext ctx =
        new AbacContext(
            UUID.randomUUID(), "user@test", "admin:write", "free", "us-east-1", "corr-1");
    AbacResult result = evaluator.evaluate(ctx);

    assertThat(result.allowed()).isFalse();
    assertThat(result.policyId()).isEqualTo(denyId);
  }

  @Test
  void evaluate_noMatchingAllowPolicy_returnsDeny() {
    Policy allow = makePolicy(UUID.randomUUID(), "admin:write", Policy.Effect.ALLOW, List.of("enterprise"), List.of());
    when(policyRepo.findByPermissionCodeAndEnabledTrue("admin:write")).thenReturn(List.of(allow));

    AbacContext ctx =
        new AbacContext(
            UUID.randomUUID(), "user@test", "admin:write", "free", "us-east-1", "corr-1");
    AbacResult result = evaluator.evaluate(ctx);

    assertThat(result.allowed()).isFalse();
    assertThat(result.reason()).isEqualTo("no_matching_allow_policy");
  }

  @Test
  void evaluate_regionFilter_worksCorrectly() {
    Policy allow = makePolicy(UUID.randomUUID(), "admin:write", Policy.Effect.ALLOW, List.of(), List.of("eu-west-1"));
    when(policyRepo.findByPermissionCodeAndEnabledTrue("admin:write")).thenReturn(List.of(allow));

    AbacContext euCtx =
        new AbacContext(
            UUID.randomUUID(), "user@test", "admin:write", "pro", "eu-west-1", "corr-1");
    assertThat(evaluator.evaluate(euCtx).allowed()).isTrue();

    AbacContext usCtx =
        new AbacContext(
            UUID.randomUUID(), "user@test", "admin:write", "pro", "us-east-1", "corr-2");
    assertThat(evaluator.evaluate(usCtx).allowed()).isFalse();
  }

  private Policy makePolicy(
      UUID id, String permCode, Policy.Effect effect, List<String> allowedPlans, List<String> allowedRegions) {
    return new Policy(
        id, permCode, effect, allowedPlans, allowedRegions, true, null, java.time.Instant.now(), java.time.Instant.now());
  }
}
