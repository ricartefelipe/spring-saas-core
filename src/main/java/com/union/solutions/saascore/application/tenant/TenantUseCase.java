package com.union.solutions.saascore.application.tenant;

import com.union.solutions.saascore.adapters.out.persistence.TenantEntity;
import com.union.solutions.saascore.adapters.out.persistence.TenantJpaRepository;
import com.union.solutions.saascore.application.abac.AuditLogger;
import com.union.solutions.saascore.application.port.OutboxPublisherPort;
import com.union.solutions.saascore.config.TenantContext;
import com.union.solutions.saascore.domain.Tenant;
import io.micrometer.core.instrument.Counter;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantUseCase {

  private final TenantJpaRepository tenantRepo;
  private final OutboxPublisherPort outboxPublisher;
  private final AuditLogger auditLogger;
  private final Counter tenantsCreatedCounter;

  public TenantUseCase(
      TenantJpaRepository tenantRepo,
      OutboxPublisherPort outboxPublisher,
      AuditLogger auditLogger,
      @Qualifier("tenantsCreatedCounter") Counter tenantsCreatedCounter) {
    this.tenantRepo = tenantRepo;
    this.outboxPublisher = outboxPublisher;
    this.auditLogger = auditLogger;
    this.tenantsCreatedCounter = tenantsCreatedCounter;
  }

  @Transactional
  public Tenant create(String name, String plan, String region) {
    UUID id = UUID.randomUUID();
    Instant now = Instant.now();
    Tenant tenant = new Tenant(id, name, plan, region, Tenant.TenantStatus.ACTIVE, now, now);
    tenantRepo.save(TenantEntity.from(tenant));
    outboxPublisher.publish(
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

  @Transactional(readOnly = true)
  public List<Tenant> searchCursor(
      Tenant.TenantStatus status,
      String plan,
      String region,
      String name,
      Instant cursor,
      int limit) {
    return tenantRepo
        .findNextPage(status, plan, region, name, cursor, PageRequest.of(0, limit))
        .stream()
        .map(TenantEntity::toDomain)
        .toList();
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
              outboxPublisher.publish(
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
              outboxPublisher.publish(
                  "TENANT",
                  id.toString(),
                  "tenant.deleted",
                  Map.of("name", entity.getName(), "plan", entity.getPlan()));
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
}
