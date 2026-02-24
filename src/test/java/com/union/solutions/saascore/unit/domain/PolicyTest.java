package com.union.solutions.saascore.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.union.solutions.saascore.domain.Policy;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PolicyTest {

  @Test
  void appliesTo_enabledNoPlanFilter_matchesAll() {
    Policy p =
        new Policy(
            UUID.randomUUID(),
            "test:read",
            Policy.Effect.ALLOW,
            List.of(),
            List.of(),
            true,
            null,
            Instant.now(),
            Instant.now());
    assertThat(p.appliesTo("free", "us-east-1")).isTrue();
    assertThat(p.appliesTo("pro", "eu-west-1")).isTrue();
  }

  @Test
  void appliesTo_disabled_neverMatches() {
    Policy p =
        new Policy(
            UUID.randomUUID(),
            "test:read",
            Policy.Effect.ALLOW,
            List.of(),
            List.of(),
            false,
            null,
            Instant.now(),
            Instant.now());
    assertThat(p.appliesTo("free", "us-east-1")).isFalse();
  }

  @Test
  void appliesTo_planFilter_matchesOnlyAllowed() {
    Policy p =
        new Policy(
            UUID.randomUUID(),
            "test:write",
            Policy.Effect.ALLOW,
            List.of("pro", "enterprise"),
            List.of(),
            true,
            null,
            Instant.now(),
            Instant.now());
    assertThat(p.appliesTo("pro", "us-east-1")).isTrue();
    assertThat(p.appliesTo("enterprise", "us-east-1")).isTrue();
    assertThat(p.appliesTo("free", "us-east-1")).isFalse();
  }

  @Test
  void appliesTo_regionFilter_matchesOnlyAllowed() {
    Policy p =
        new Policy(
            UUID.randomUUID(),
            "test:write",
            Policy.Effect.ALLOW,
            List.of(),
            List.of("eu-west-1"),
            true,
            null,
            Instant.now(),
            Instant.now());
    assertThat(p.appliesTo("pro", "eu-west-1")).isTrue();
    assertThat(p.appliesTo("pro", "us-east-1")).isFalse();
  }

  @Test
  void appliesTo_bothFilters_requiresBothMatch() {
    Policy p =
        new Policy(
            UUID.randomUUID(),
            "test:write",
            Policy.Effect.ALLOW,
            List.of("pro"),
            List.of("eu-west-1"),
            true,
            null,
            Instant.now(),
            Instant.now());
    assertThat(p.appliesTo("pro", "eu-west-1")).isTrue();
    assertThat(p.appliesTo("pro", "us-east-1")).isFalse();
    assertThat(p.appliesTo("free", "eu-west-1")).isFalse();
  }

  @Test
  void equals_sameId_areEqual() {
    UUID id = UUID.randomUUID();
    Policy p1 =
        new Policy(id, "a", Policy.Effect.ALLOW, List.of(), List.of(), true, null, null, null);
    Policy p2 =
        new Policy(id, "b", Policy.Effect.DENY, List.of(), List.of(), false, null, null, null);
    assertThat(p1).isEqualTo(p2);
    assertThat(p1.hashCode()).isEqualTo(p2.hashCode());
  }

  @Test
  void equals_differentId_areNotEqual() {
    Policy p1 =
        new Policy(
            UUID.randomUUID(),
            "a",
            Policy.Effect.ALLOW,
            List.of(),
            List.of(),
            true,
            null,
            null,
            null);
    Policy p2 =
        new Policy(
            UUID.randomUUID(),
            "a",
            Policy.Effect.ALLOW,
            List.of(),
            List.of(),
            true,
            null,
            null,
            null);
    assertThat(p1).isNotEqualTo(p2);
  }
}
