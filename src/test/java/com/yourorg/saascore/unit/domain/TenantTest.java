package com.yourorg.saascore.unit.domain;

import com.yourorg.saascore.domain.Tenant;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TenantTest {

    @Test
    void isActive_whenActive_returnsTrue() {
        Tenant t = new Tenant(
                UUID.randomUUID(),
                "Acme",
                Tenant.TenantStatus.ACTIVE,
                Tenant.Plan.pro,
                "region-a",
                "shard-a",
                Instant.now());
        assertThat(t.isActive()).isTrue();
    }

    @Test
    void isActive_whenSuspended_returnsFalse() {
        Tenant t = new Tenant(
                UUID.randomUUID(),
                "Acme",
                Tenant.TenantStatus.SUSPENDED,
                Tenant.Plan.free,
                "region-a",
                "shard-a",
                Instant.now());
        assertThat(t.isActive()).isFalse();
    }
}
