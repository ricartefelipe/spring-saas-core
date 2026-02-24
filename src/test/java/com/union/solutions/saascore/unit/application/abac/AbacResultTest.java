package com.union.solutions.saascore.unit.application.abac;

import static org.assertj.core.api.Assertions.assertThat;

import com.union.solutions.saascore.application.abac.AbacResult;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AbacResultTest {

  @Test
  void allow_returnsAllowedTrue() {
    AbacResult result = AbacResult.allow();
    assertThat(result.allowed()).isTrue();
    assertThat(result.policyId()).isNull();
  }

  @Test
  void deny_returnsAllowedFalse() {
    UUID policyId = UUID.randomUUID();
    AbacResult result = AbacResult.deny(policyId, "denied_by_policy");
    assertThat(result.allowed()).isFalse();
    assertThat(result.policyId()).isEqualTo(policyId);
    assertThat(result.reason()).isEqualTo("denied_by_policy");
  }

  @Test
  void deny_withNullPolicyId() {
    AbacResult result = AbacResult.deny(null, "no_matching_allow_policy");
    assertThat(result.allowed()).isFalse();
    assertThat(result.policyId()).isNull();
    assertThat(result.reason()).isEqualTo("no_matching_allow_policy");
  }
}
