package com.union.solutions.saascore.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.union.solutions.saascore.application.abac.AuditLogger;
import com.union.solutions.saascore.application.port.OutboxPublisherPort;
import com.union.solutions.saascore.application.port.PolicyRepository;
import com.union.solutions.saascore.config.TenantContext;
import com.union.solutions.saascore.domain.Policy;
import io.micrometer.core.instrument.Counter;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PolicyService {

  private final PolicyRepository repo;
  private final OutboxPublisherPort outboxPublisher;
  private final AuditLogger auditLogger;
  private final ObjectMapper objectMapper;
  private final Counter policiesUpdatedCounter;

  public PolicyService(
      PolicyRepository repo,
      OutboxPublisherPort outboxPublisher,
      AuditLogger auditLogger,
      ObjectMapper objectMapper,
      @Qualifier("policiesUpdatedCounter") Counter policiesUpdatedCounter) {
    this.repo = repo;
    this.outboxPublisher = outboxPublisher;
    this.auditLogger = auditLogger;
    this.objectMapper = objectMapper;
    this.policiesUpdatedCounter = policiesUpdatedCounter;
  }

  @Transactional
  public Policy create(
      String permissionCode,
      Policy.Effect effect,
      List<String> allowedPlans,
      List<String> allowedRegions,
      boolean enabled,
      String notes) {
    Policy policy =
        new Policy(
            UUID.randomUUID(),
            permissionCode,
            effect,
            allowedPlans != null ? allowedPlans : List.of(),
            allowedRegions != null ? allowedRegions : List.of(),
            enabled,
            notes,
            Instant.now(),
            Instant.now());
    Policy saved = repo.save(policy);
    outboxPublisher.publish(
        "POLICY",
        saved.getId().toString(),
        "policy.created",
        Map.of("permissionCode", permissionCode, "effect", effect.name()));
    auditLogger.log(
        TenantContext.getTenantId().orElse(null),
        TenantContext.getSubject(),
        TenantContext.getRoles().toString(),
        TenantContext.getPerms().toString(),
        "POLICY_CREATED",
        "policy",
        saved.getId().toString(),
        null,
        null,
        201,
        TenantContext.getCorrelationId(),
        null);
    return saved;
  }

  @Transactional(readOnly = true)
  public Page<Policy> search(
      String permissionCode, Policy.Effect effect, Boolean enabled, Pageable pageable) {
    return repo.search(permissionCode, effect, enabled, pageable);
  }

  @Transactional(readOnly = true)
  public Optional<Policy> getById(UUID id) {
    return repo.findActiveById(id);
  }

  @Transactional
  public Optional<Policy> update(
      UUID id,
      String permissionCode,
      Policy.Effect effect,
      List<String> allowedPlans,
      List<String> allowedRegions,
      Boolean enabled,
      String notes) {
    return repo.findActiveById(id)
        .map(
            policy -> {
              if (permissionCode != null) policy.setPermissionCode(permissionCode);
              if (effect != null) policy.setEffect(effect);
              if (allowedPlans != null) policy.setAllowedPlans(allowedPlans);
              if (allowedRegions != null) policy.setAllowedRegions(allowedRegions);
              if (enabled != null) policy.setEnabled(enabled);
              if (notes != null) policy.setNotes(notes);
              policy.setUpdatedAt(Instant.now());
              Policy saved = repo.save(policy);
              policiesUpdatedCounter.increment();
              outboxPublisher.publish(
                  "POLICY",
                  id.toString(),
                  "policy.updated",
                  Map.of(
                      "permissionCode", saved.getPermissionCode(),
                      "effect", saved.getEffect().name()));
              auditLogger.log(
                  TenantContext.getTenantId().orElse(null),
                  TenantContext.getSubject(),
                  TenantContext.getRoles().toString(),
                  TenantContext.getPerms().toString(),
                  "POLICY_UPDATED",
                  "policy",
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
  public boolean softDelete(UUID id) {
    Optional<Policy> found = repo.findActiveById(id);
    if (found.isEmpty()) return false;
    Policy policy = found.get();
    repo.softDelete(id);
    outboxPublisher.publish(
        "POLICY",
        id.toString(),
        "policy.deleted",
        Map.of("permissionCode", policy.getPermissionCode()));
    auditLogger.log(
        TenantContext.getTenantId().orElse(null),
        TenantContext.getSubject(),
        TenantContext.getRoles().toString(),
        TenantContext.getPerms().toString(),
        "POLICY_DELETED",
        "policy",
        id.toString(),
        null,
        null,
        204,
        TenantContext.getCorrelationId(),
        null);
    return true;
  }

  @Transactional(readOnly = true)
  public List<Policy> getApplicablePolicies(String plan, String region) {
    return repo.findByEnabledTrue().stream()
        .filter(p -> p.appliesTo(plan, region))
        .toList();
  }

  @Transactional(readOnly = true)
  public long countActive() {
    return repo.countActive();
  }
}
