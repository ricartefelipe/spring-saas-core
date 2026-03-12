package com.union.solutions.saascore.unit.adapters.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.union.solutions.saascore.adapters.out.persistence.TenantEntity;
import com.union.solutions.saascore.domain.Tenant;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TenantEntityTest {

  @Test
  void from_toDomain_roundTrip() {
    UUID id = UUID.randomUUID();
    Instant now = Instant.now();
    Tenant tenant =
        new Tenant(id, "Acme", "pro", "us-east-1", Tenant.TenantStatus.ACTIVE, now, now);

    TenantEntity entity = TenantEntity.from(tenant);
    assertThat(entity.getId()).isEqualTo(id);
    assertThat(entity.getName()).isEqualTo("Acme");

    Tenant back = entity.toDomain();
    assertThat(back.getId()).isEqualTo(id);
    assertThat(back.getName()).isEqualTo("Acme");
    assertThat(back.getPlan()).isEqualTo("pro");
    assertThat(back.getRegion()).isEqualTo("us-east-1");
    assertThat(back.getStatus()).isEqualTo(Tenant.TenantStatus.ACTIVE);
  }

  @Test
  void equals_sameId() {
    UUID id = UUID.randomUUID();
    TenantEntity e1 =
        TenantEntity.from(
            new Tenant(
                id, "A", "pro", "us", Tenant.TenantStatus.ACTIVE, Instant.now(), Instant.now()));
    TenantEntity e2 =
        TenantEntity.from(
            new Tenant(
                id, "B", "free", "eu", Tenant.TenantStatus.DELETED, Instant.now(), Instant.now()));
    assertThat(e1).isEqualTo(e2);
  }

  @Test
  void equals_differentId() {
    TenantEntity e1 =
        TenantEntity.from(
            new Tenant(
                UUID.randomUUID(),
                "A",
                "pro",
                "us",
                Tenant.TenantStatus.ACTIVE,
                Instant.now(),
                Instant.now()));
    TenantEntity e2 =
        TenantEntity.from(
            new Tenant(
                UUID.randomUUID(),
                "A",
                "pro",
                "us",
                Tenant.TenantStatus.ACTIVE,
                Instant.now(),
                Instant.now()));
    assertThat(e1).isNotEqualTo(e2);
  }
}
