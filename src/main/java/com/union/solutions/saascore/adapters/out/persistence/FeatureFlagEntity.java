package com.union.solutions.saascore.adapters.out.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "feature_flags")
public class FeatureFlagEntity {

  @Id
  @Column(name = "id")
  private UUID id;

  @Column(name = "tenant_id", nullable = false)
  private UUID tenantId;

  @Column(name = "name", nullable = false, length = 128)
  private String name;

  @Column(name = "enabled", nullable = false)
  private boolean enabled;

  @Column(name = "rollout_percent")
  private int rolloutPercent;

  @Column(name = "allowed_roles", columnDefinition = "text")
  private String allowedRoles;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "deleted", nullable = false)
  private boolean deleted = false;

  @Column(name = "deleted_at")
  private Instant deletedAt;

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
    this.rolloutPercent = rolloutPercent;
  }

  public String getAllowedRoles() {
    return allowedRoles;
  }

  public void setAllowedRoles(String allowedRoles) {
    this.allowedRoles = allowedRoles;
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

  public boolean isDeleted() {
    return deleted;
  }

  public void setDeleted(boolean deleted) {
    this.deleted = deleted;
  }

  public Instant getDeletedAt() {
    return deletedAt;
  }

  public void setDeletedAt(Instant deletedAt) {
    this.deletedAt = deletedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof FeatureFlagEntity that)) return false;
    return id != null && java.util.Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
