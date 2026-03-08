package com.union.solutions.saascore.application.service;

import com.union.solutions.saascore.application.abac.AuditLogger;
import com.union.solutions.saascore.application.port.FeatureFlagRepository;
import com.union.solutions.saascore.application.port.OutboxPublisherPort;
import com.union.solutions.saascore.config.TenantContext;
import com.union.solutions.saascore.domain.FeatureFlag;
import io.micrometer.core.instrument.Counter;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeatureFlagService {

  private final FeatureFlagRepository repo;
  private final OutboxPublisherPort outboxPublisher;
  private final AuditLogger auditLogger;
  private final Counter flagsToggledCounter;

  public FeatureFlagService(
      FeatureFlagRepository repo,
      OutboxPublisherPort outboxPublisher,
      AuditLogger auditLogger,
      @Qualifier("flagsToggledCounter") Counter flagsToggledCounter) {
    this.repo = repo;
    this.outboxPublisher = outboxPublisher;
    this.auditLogger = auditLogger;
    this.flagsToggledCounter = flagsToggledCounter;
  }

  @Transactional
  public FeatureFlag create(
      UUID tenantId, String name, boolean enabled, int rolloutPercent, List<String> allowedRoles) {
    FeatureFlag saved =
        repo.createOrResurrect(tenantId, name, enabled, rolloutPercent, allowedRoles);
    outboxPublisher.publish(
        "FLAG",
        saved.getId().toString(),
        "flag.created",
        Map.of("tenantId", tenantId.toString(), "name", name));
    auditLogger.log(
        tenantId,
        TenantContext.getSubject(),
        TenantContext.getRoles().toString(),
        TenantContext.getPerms().toString(),
        "FLAG_CREATED",
        "feature_flag",
        name,
        null,
        null,
        201,
        TenantContext.getCorrelationId(),
        null);
    return saved;
  }

  @Transactional(readOnly = true)
  public List<FeatureFlag> listByTenant(UUID tenantId) {
    return repo.findByTenantId(tenantId);
  }

  @Transactional
  public Optional<FeatureFlag> update(
      UUID tenantId,
      String name,
      Boolean enabled,
      Integer rolloutPercent,
      List<String> allowedRoles) {
    return repo.findByTenantIdAndName(tenantId, name)
        .map(
            flag -> {
              if (enabled != null) flag.setEnabled(enabled);
              if (rolloutPercent != null) flag.setRolloutPercent(rolloutPercent);
              if (allowedRoles != null) flag.setAllowedRoles(allowedRoles);
              flag.setUpdatedAt(Instant.now());
              FeatureFlag saved = repo.save(flag);
              flagsToggledCounter.increment();
              outboxPublisher.publish(
                  "FLAG",
                  saved.getId().toString(),
                  "flag.toggled",
                  Map.of("tenantId", tenantId.toString(), "name", name));
              auditLogger.log(
                  tenantId,
                  TenantContext.getSubject(),
                  TenantContext.getRoles().toString(),
                  TenantContext.getPerms().toString(),
                  "FLAG_UPDATED",
                  "feature_flag",
                  name,
                  null,
                  null,
                  200,
                  TenantContext.getCorrelationId(),
                  null);
              return saved;
            });
  }

  @Transactional
  public boolean softDelete(UUID tenantId, String name) {
    Optional<FeatureFlag> found = repo.findByTenantIdAndName(tenantId, name);
    if (found.isEmpty()) return false;
    FeatureFlag flag = found.get();
    repo.softDelete(tenantId, name);
    outboxPublisher.publish(
        "FLAG",
        flag.getId().toString(),
        "flag.deleted",
        Map.of("tenantId", tenantId.toString(), "name", name));
    auditLogger.log(
        tenantId,
        TenantContext.getSubject(),
        TenantContext.getRoles().toString(),
        TenantContext.getPerms().toString(),
        "FLAG_DELETED",
        "feature_flag",
        name,
        null,
        null,
        204,
        TenantContext.getCorrelationId(),
        null);
    return true;
  }

  @Transactional(readOnly = true)
  public long countActiveFlags() {
    return repo.countActiveFlags();
  }
}
