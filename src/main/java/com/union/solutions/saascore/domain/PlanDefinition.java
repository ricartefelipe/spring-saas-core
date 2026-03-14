package com.union.solutions.saascore.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class PlanDefinition {

  private UUID id;
  private String slug;
  private String displayName;
  private String description;
  private long monthlyPriceCents;
  private long yearlyPriceCents;
  private int maxUsers;
  private int maxProjects;
  private int storageGb;
  private boolean active;
  private Instant createdAt;
  private Instant updatedAt;
  private String stripePriceIdMonthly;
  private String stripePriceIdYearly;

  public PlanDefinition() {}

  public PlanDefinition(
      UUID id,
      String slug,
      String displayName,
      String description,
      long monthlyPriceCents,
      long yearlyPriceCents,
      int maxUsers,
      int maxProjects,
      int storageGb,
      boolean active,
      Instant createdAt,
      Instant updatedAt) {
    this.id = id;
    this.slug = slug;
    this.displayName = displayName;
    this.description = description;
    this.monthlyPriceCents = monthlyPriceCents;
    this.yearlyPriceCents = yearlyPriceCents;
    this.maxUsers = maxUsers;
    this.maxProjects = maxProjects;
    this.storageGb = storageGb;
    this.active = active;
    this.createdAt = createdAt != null ? createdAt : Instant.now();
    this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getSlug() {
    return slug;
  }

  public void setSlug(String slug) {
    this.slug = slug;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public long getMonthlyPriceCents() {
    return monthlyPriceCents;
  }

  public void setMonthlyPriceCents(long monthlyPriceCents) {
    this.monthlyPriceCents = monthlyPriceCents;
  }

  public long getYearlyPriceCents() {
    return yearlyPriceCents;
  }

  public void setYearlyPriceCents(long yearlyPriceCents) {
    this.yearlyPriceCents = yearlyPriceCents;
  }

  public int getMaxUsers() {
    return maxUsers;
  }

  public void setMaxUsers(int maxUsers) {
    this.maxUsers = maxUsers;
  }

  public int getMaxProjects() {
    return maxProjects;
  }

  public void setMaxProjects(int maxProjects) {
    this.maxProjects = maxProjects;
  }

  public int getStorageGb() {
    return storageGb;
  }

  public void setStorageGb(int storageGb) {
    this.storageGb = storageGb;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
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

  public String getStripePriceIdMonthly() {
    return stripePriceIdMonthly;
  }

  public void setStripePriceIdMonthly(String stripePriceIdMonthly) {
    this.stripePriceIdMonthly = stripePriceIdMonthly;
  }

  public String getStripePriceIdYearly() {
    return stripePriceIdYearly;
  }

  public void setStripePriceIdYearly(String stripePriceIdYearly) {
    this.stripePriceIdYearly = stripePriceIdYearly;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PlanDefinition that)) return false;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
