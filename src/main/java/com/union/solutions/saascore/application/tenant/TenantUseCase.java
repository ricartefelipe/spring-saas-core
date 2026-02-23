package com.union.solutions.saascore.application.tenant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.union.solutions.saascore.adapters.out.persistence.OutboxEventEntity;
import com.union.solutions.saascore.adapters.out.persistence.OutboxEventJpaRepository;
import com.union.solutions.saascore.adapters.out.persistence.TenantEntity;
import com.union.solutions.saascore.adapters.out.persistence.TenantJpaRepository;
import com.union.solutions.saascore.application.abac.AuditLogger;
import com.union.solutions.saascore.config.TenantContext;
import com.union.solutions.saascore.domain.Tenant;
import io.micrometer.core.instrument.Counter;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantUseCase {

  private final TenantJpaRepository tenantRepo;
  private final OutboxEventJpaRepository outboxRepo;
  private final AuditLogger auditLogger;
  private final ObjectMapper objectMapper;
  private final Counter tenantsCreatedCounter;

  public TenantUseCase(
      TenantJpaRepository tenantRepo,
      OutboxEventJpaRepository outboxRepo,
      AuditLogger auditLogger,
      ObjectMapper objectMapper,
      @Qualifier("tenantsCreatedCounter") Counter tenantsCreatedCounter) {
    this.tenantRepo = tenantRepo;
    this.outboxRepo = outboxRepo;
    this.auditLogger = auditLogger;
    this.objectMapper = objectMapper;
    this.tenantsCreatedCounter = tenantsCreatedCounter;
  }

  @Transactional
  public Tenant create(String name, String plan, String region) {
    UUID id = UUID.randomUUID();
    Instant now = Instant.now();
    Tenant tenant = new Tenant(id, name, plan, region, Tenant.TenantStatus.ACTIVE, now, now);
    tenantRepo.save(TenantEntity.from(tenant));
    publishOutbox(
        "TENANT",
        id.toString(),
        "tenant.created",
        Map.of("name", name, "plan", plan, "region", region));
    tenantsCreatedCounter.increment();
    auditLogger.log(
        TenantContext.getTenantId().orElse(null),
        TenantContext.getSubject(),
        TenantContext.getRoles().toString(),
        TenantContext.getPerms().toString(),
        "TENANT_CREATED",
        "tenant",
        id.toString(),
        null,
        null,
        201,
        TenantContext.getCorrelationId(),
        null);
    return tenant;
  }

  @Transactional(readOnly = true)
  public Optional<Tenant> getById(UUID id) {
    return tenantRepo.findById(id).map(TenantEntity::toDomain);
  }

  @Transactional(readOnly = true)
  public Page<Tenant> search(
      Tenant.TenantStatus status, String plan, String region, String name, Pageable pageable) {
    return tenantRepo.search(status, plan, region, name, pageable).map(TenantEntity::toDomain);
  }

  @Transactional
  public Optional<Tenant> update(
      UUID id, String name, String plan, String region, Tenant.TenantStatus status) {
    return tenantRepo
        .findById(id)
        .map(
            entity -> {
              if (name != null) entity.setName(name);
              if (plan != null) entity.setPlan(plan);
              if (region != null) entity.setRegion(region);
              if (status != null) entity.setStatus(status);
              entity.setUpdatedAt(Instant.now());
              tenantRepo.save(entity);
              publishOutbox(
                  "TENANT",
                  id.toString(),
                  "tenant.updated",
                  Map.of("name", entity.getName(), "plan", entity.getPlan()));
              auditLogger.log(
                  TenantContext.getTenantId().orElse(null),
                  TenantContext.getSubject(),
                  TenantContext.getRoles().toString(),
                  TenantContext.getPerms().toString(),
                  "TENANT_UPDATED",
                  "tenant",
                  id.toString(),
                  null,
                  null,
                  200,
                  TenantContext.getCorrelationId(),
                  null);
              return entity.toDomain();
            });
  }

  @Transactional
  public boolean softDelete(UUID id) {
    return tenantRepo
        .findById(id)
        .map(
            entity -> {
              entity.setStatus(Tenant.TenantStatus.DELETED);
              entity.setUpdatedAt(Instant.now());
              tenantRepo.save(entity);
              auditLogger.log(
                  TenantContext.getTenantId().orElse(null),
                  TenantContext.getSubject(),
                  TenantContext.getRoles().toString(),
                  TenantContext.getPerms().toString(),
                  "TENANT_DELETED",
                  "tenant",
                  id.toString(),
                  null,
                  null,
                  204,
                  TenantContext.getCorrelationId(),
                  null);
              return true;
            })
        .orElse(false);
  }

  private void publishOutbox(
      String aggregateType, String aggregateId, String eventType, Map<String, String> data) {
    OutboxEventEntity outbox = new OutboxEventEntity();
    outbox.setId(UUID.randomUUID());
    outbox.setAggregateType(aggregateType);
    outbox.setAggregateId(aggregateId);
    outbox.setEventType(eventType);
    outbox.setPayload(writeJson(data));
    outbox.setStatus("PENDING");
    outbox.setAttempts(0);
    Instant now = Instant.now();
    outbox.setCreatedAt(now);
    outbox.setUpdatedAt(now);
    outboxRepo.save(outbox);
  }

  private String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      return "{}";
    }
  }
}
