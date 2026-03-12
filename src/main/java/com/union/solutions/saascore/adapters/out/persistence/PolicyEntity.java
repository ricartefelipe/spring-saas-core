package com.union.solutions.saascore.adapters.out.persistence;

import com.union.solutions.saascore.domain.Policy;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "policies")
public class PolicyEntity {

  @Id
  @Column(name = "id")
  private UUID id;

  @Column(name = "permission_code", nullable = false, length = 128)
  private String permissionCode;

  @Enumerated(EnumType.STRING)
  @Column(name = "effect", nullable = false, length = 16)
  private Policy.Effect effect;

  @Column(name = "allowed_plans", columnDefinition = "text")
  private String allowedPlans;

  @Column(name = "allowed_regions", columnDefinition = "text")
  private String allowedRegions;

  @Column(name = "enabled", nullable = false)
  private boolean enabled;

  @Column(name = "notes", columnDefinition = "text")
  private String notes;

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

  public String getPermissionCode() {
    return permissionCode;
  }

  public void setPermissionCode(String permissionCode) {
    this.permissionCode = permissionCode;
  }

  public Policy.Effect getEffect() {
    return effect;
  }

  public void setEffect(Policy.Effect effect) {
    this.effect = effect;
  }

  public String getAllowedPlans() {
    return allowedPlans;
  }

  public void setAllowedPlans(String allowedPlans) {
    this.allowedPlans = allowedPlans;
  }

  public String getAllowedRegions() {
    return allowedRegions;
  }

  public void setAllowedRegions(String allowedRegions) {
    this.allowedRegions = allowedRegions;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
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
    if (!(o instanceof PolicyEntity that)) return false;
    return id != null && java.util.Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
