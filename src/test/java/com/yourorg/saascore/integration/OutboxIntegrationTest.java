package com.yourorg.saascore.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.yourorg.saascore.config.DataSourceRoutingConfig;
import com.yourorg.saascore.config.TenantContext;
import com.yourorg.saascore.application.tenant.TenantUseCase;
import com.yourorg.saascore.adapters.out.persistence.OutboxEventJpaRepository;
import com.yourorg.saascore.domain.OutboxEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
class OutboxIntegrationTest {

    @Autowired
    TenantUseCase tenantUseCase;

    @Autowired
    OutboxEventJpaRepository outboxRepo;

    @Test
    @Transactional
    void createTenant_persistsOutboxEventInSameTransaction() {
        TenantContext.setShardKey(DataSourceRoutingConfig.SHARD_A);
        try {
            com.yourorg.saascore.domain.Tenant t =
                    tenantUseCase.create("Outbox Test Tenant", com.yourorg.saascore.domain.Tenant.Plan.free, "region-a", "shard-a");
            assertThat(t.getId()).isNotNull();

            var pending =
                    outboxRepo.findPendingUnlocked(
                            java.time.Instant.now().minusSeconds(120),
                            org.springframework.data.domain.PageRequest.of(0, 10));
            assertThat(pending).isNotEmpty();
            assertThat(pending.stream().anyMatch(e -> e.getType().equals("tenant.created"))).isTrue();
            assertThat(pending.stream().anyMatch(e -> e.getStatus() == OutboxEvent.OutboxStatus.PENDING))
                    .isTrue();
        } finally {
            TenantContext.clear();
        }
    }
}
