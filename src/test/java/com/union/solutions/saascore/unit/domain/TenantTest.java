package com.union.solutions.saascore.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.union.solutions.saascore.domain.Tenant;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TenantTest {

  @Test
  void isActive_whenActive_returnsTrue() {
    Tenant t =
        new Tenant(
            UUID.randomUUID(),
            "Acme",
            "pro",
            "us-east-1",
            Tenant.TenantStatus.ACTIVE,
            Instant.now(),
            Instant.now());
    assertThat(t.isActive()).isTrue();
  }

  @Test
  void isActive_whenSuspended_returnsFalse() {
    Tenant t =
        new Tenant(
            UUID.randomUUID(),
            "Acme",
            "free",
            "us-east-1",
            Tenant.TenantStatus.SUSPENDED,
            Instant.now(),
            Instant.now());
    assertThat(t.isActive()).isFalse();
  }

  @Test
  void isActive_whenDeleted_returnsFalse() {
    Tenant t =
        new Tenant(
            UUID.randomUUID(),
            "Acme",
            "pro",
            "eu-west-1",
            Tenant.TenantStatus.DELETED,
            Instant.now(),
            Instant.now());
    assertThat(t.isActive()).isFalse();
  }
}
