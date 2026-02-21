package com.yourorg.saascore.application.tenant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourorg.saascore.domain.Tenant;
import com.yourorg.saascore.adapters.out.persistence.TenantEntity;
import com.yourorg.saascore.adapters.out.persistence.TenantJpaRepository;
import com.yourorg.saascore.adapters.out.persistence.OutboxEventEntity;
import com.yourorg.saascore.adapters.out.persistence.OutboxEventJpaRepository;
import com.yourorg.saascore.domain.OutboxEvent;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantUseCase {

    private final TenantJpaRepository tenantRepo;
    private final OutboxEventJpaRepository outboxRepo;
    private final ObjectMapper objectMapper;
    private final String appRegion;

    public TenantUseCase(
            TenantJpaRepository tenantRepo,
            OutboxEventJpaRepository outboxRepo,
            ObjectMapper objectMapper,
            @Value("${app.region}") String appRegion) {
        this.tenantRepo = tenantRepo;
        this.outboxRepo = outboxRepo;
        this.objectMapper = objectMapper;
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
                        writeJson(Map.of("name", name)),
                        OutboxEvent.OutboxStatus.PENDING,
                        0,
                        null,
                        null,
                        Instant.now(),
                        null);
        outboxRepo.save(OutboxEventEntity.from(outbox));
        return tenant;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize JSON", e);
        }
    }

    @Transactional(readOnly = true)
    public Optional<Tenant> getById(UUID id) {
        return tenantRepo.findById(id).map(TenantEntity::toDomain);
    }
}
