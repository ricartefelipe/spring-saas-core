package com.union.solutions.saascore.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Domínio de feature flag por tenant. Usado pela camada de aplicação; conversão para
 * FeatureFlagEntity fica nos adapters out.
 */
public class FeatureFlag {

  private UUID id;
  private UUID tenantId;
  private String name;
  private boolean enabled;
  private int rolloutPercent;
  private List<String> allowedRoles;
  private Instant createdAt;
  private Instant updatedAt;

  public FeatureFlag() {}

  public FeatureFlag(
      UUID id,
      UUID tenantId,
      String name,
      boolean enabled,
      int rolloutPercent,
      List<String> allowedRoles,
      Instant createdAt,
      Instant updatedAt) {
    this.id = id;
    this.tenantId = tenantId;
    this.name = name;
    this.enabled = enabled;
    this.rolloutPercent = Math.max(0, Math.min(100, rolloutPercent));
    this.allowedRoles = allowedRoles != null ? allowedRoles : List.of();
    this.createdAt = createdAt != null ? createdAt : Instant.now();
    this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public void setTenantId(UUID tenantId) {
    this.tenantId = tenantId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public int getRolloutPercent() {
    return rolloutPercent;
  }

  public void setRolloutPercent(int rolloutPercent) {
    this.rolloutPercent = Math.max(0, Math.min(100, rolloutPercent));
  }

  public List<String> getAllowedRoles() {
    return allowedRoles;
  }

  public void setAllowedRoles(List<String> allowedRoles) {
    this.allowedRoles = allowedRoles != null ? allowedRoles : List.of();
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
