package com.union.solutions.saascore.application.tenant;

import com.union.solutions.saascore.application.port.AuditLogger;
import com.union.solutions.saascore.application.port.OutboxPublisherPort;
import com.union.solutions.saascore.application.port.TenantRepository;
import com.union.solutions.saascore.config.TenantContext;
import com.union.solutions.saascore.domain.Tenant;
import io.micrometer.core.instrument.Counter;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantUseCase {

  private final TenantRepository tenantRepo;
  private final OutboxPublisherPort outboxPublisher;
  private final AuditLogger auditLogger;
  private final Counter tenantsCreatedCounter;

  public TenantUseCase(
      TenantRepository tenantRepo,
      OutboxPublisherPort outboxPublisher,
      AuditLogger auditLogger,
      @Qualifier("tenantsCreatedCounter") Counter tenantsCreatedCounter) {
    this.tenantRepo = tenantRepo;
    this.outboxPublisher = outboxPublisher;
    this.auditLogger = auditLogger;
    this.tenantsCreatedCounter = tenantsCreatedCounter;
  }

  @Transactional
  @CacheEvict(cacheNames = "frontTenants", allEntries = true)
  public Tenant create(String name, String plan, String region) {
    UUID id = UUID.randomUUID();
    Instant now = Instant.now();
    Tenant tenant = new Tenant(id, name, plan, region, Tenant.TenantStatus.ACTIVE, now, now);
    tenantRepo.save(tenant);
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
    return tenantRepo.findById(id);
  }

  @Transactional(readOnly = true)
  public Page<Tenant> search(
      Tenant.TenantStatus status, String plan, String region, String name, Pageable pageable) {
    return tenantRepo.search(status, plan, region, name, pageable);
  }

  @Transactional(readOnly = true)
  @Cacheable(
      cacheNames = "frontTenants",
      key = "(#status != null ? #status.toString() : '') + '-' + (#plan != null ? #plan : '') + '-' + (#region != null ? #region : '') + '-' + (#name != null ? #name : '') + '-' + (#cursor != null ? #cursor.toString() : '') + '-' + #limit")
  public List<Tenant> searchCursor(
      Tenant.TenantStatus status,
      String plan,
      String region,
      String name,
      Instant cursor,
      int limit) {
    return tenantRepo.findNextPage(status, plan, region, name, cursor, PageRequest.of(0, limit));
  }

  @Transactional
  @CacheEvict(cacheNames = "frontTenants", allEntries = true)
  public Optional<Tenant> update(
      UUID id, String name, String plan, String region, Tenant.TenantStatus status) {
    return tenantRepo
        .findById(id)
        .map(
            tenant -> {
              if (name != null) tenant.setName(name);
              if (plan != null) tenant.setPlan(plan);
              if (region != null) tenant.setRegion(region);
              if (status != null) tenant.setStatus(status);
              tenant.setUpdatedAt(Instant.now());
              Tenant saved = tenantRepo.save(tenant);
              outboxPublisher.publish(
                  "TENANT",
                  id.toString(),
                  "tenant.updated",
                  Map.of("name", saved.getName(), "plan", saved.getPlan()));
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
              return saved;
            });
  }

  @Transactional
  @CacheEvict(cacheNames = "frontTenants", allEntries = true)
  public boolean softDelete(UUID id) {
    return tenantRepo
        .findById(id)
        .map(
            tenant -> {
              tenant.setStatus(Tenant.TenantStatus.DELETED);
              tenant.setUpdatedAt(Instant.now());
              tenantRepo.save(tenant);
              outboxPublisher.publish(
                  "TENANT",
                  id.toString(),
                  "tenant.deleted",
                  Map.of("name", tenant.getName(), "plan", tenant.getPlan()));
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
