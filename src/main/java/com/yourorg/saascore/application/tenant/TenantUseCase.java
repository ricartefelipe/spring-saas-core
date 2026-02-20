package com.yourorg.saascore.application.tenant;

import com.yourorg.saascore.config.DataSourceRoutingConfig;
import com.yourorg.saascore.config.TenantContext;
import com.yourorg.saascore.domain.Tenant;
import com.yourorg.saascore.adapters.out.persistence.TenantEntity;
import com.yourorg.saascore.adapters.out.persistence.TenantJpaRepository;
import com.yourorg.saascore.adapters.out.persistence.OutboxEventEntity;
import com.yourorg.saascore.adapters.out.persistence.OutboxEventJpaRepository;
import com.yourorg.saascore.domain.OutboxEvent;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantUseCase {

    private final TenantJpaRepository tenantRepo;
    private final OutboxEventJpaRepository outboxRepo;
    private final String appRegion;

    public TenantUseCase(
            TenantJpaRepository tenantRepo,
            OutboxEventJpaRepository outboxRepo,
            @Value("${app.region}") String appRegion) {
        this.tenantRepo = tenantRepo;
        this.outboxRepo = outboxRepo;
        this.appRegion = appRegion;
    }

    @Transactional
    public Tenant create(String name, Tenant.Plan plan, String primaryRegion, String shardKey) {
        UUID id = UUID.randomUUID();
        Tenant tenant =
                new Tenant(
                        id,
                        name,
                        Tenant.TenantStatus.ACTIVE,
                        plan,
                        primaryRegion,
                        shardKey,
                        Instant.now());
        TenantEntity e = TenantEntity.from(tenant);
        tenantRepo.save(e);
        OutboxEvent outbox =
                new OutboxEvent(
                        UUID.randomUUID(),
                        id,
                        appRegion,
                        "Tenant",
                        id.toString(),
                        "tenant.created",
                        "{\"name\":\"" + name + "\"}",
                        OutboxEvent.OutboxStatus.PENDING,
                        0,
                        null,
                        null,
                        Instant.now(),
                        null);
        outboxRepo.save(OutboxEventEntity.from(outbox));
        return tenant;
    }

    @Transactional(readOnly = true)
    public Optional<Tenant> getById(UUID id) {
        return tenantRepo.findById(id).map(TenantEntity::toDomain);
    }
}
