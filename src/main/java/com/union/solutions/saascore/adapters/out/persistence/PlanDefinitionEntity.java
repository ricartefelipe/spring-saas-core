package com.union.solutions.saascore.adapters.out.persistence;

import com.union.solutions.saascore.domain.PlanDefinition;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "plan_definitions")
public class PlanDefinitionEntity {

  @Id private UUID id;

  @Column(nullable = false, unique = true, length = 50)
  private String slug;

  @Column(name = "display_name", nullable = false, length = 100)
  private String displayName;

  @Column(length = 500)
  private String description;

  @Column(name = "monthly_price_cents", nullable = false)
  private long monthlyPriceCents;

  @Column(name = "yearly_price_cents", nullable = false)
  private long yearlyPriceCents;

  @Column(name = "max_users", nullable = false)
  private int maxUsers;

  @Column(name = "max_projects", nullable = false)
  private int maxProjects;

  @Column(name = "storage_gb", nullable = false)
  private int storageGb;

  @Column(nullable = false)
  private boolean active;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "stripe_price_id_monthly")
  private String stripePriceIdMonthly;

  @Column(name = "stripe_price_id_yearly")
  private String stripePriceIdYearly;

  public static PlanDefinitionEntity fromDomain(PlanDefinition d) {
    PlanDefinitionEntity e = new PlanDefinitionEntity();
    e.id = d.getId();
    e.slug = d.getSlug();
    e.displayName = d.getDisplayName();
    e.description = d.getDescription();
    e.monthlyPriceCents = d.getMonthlyPriceCents();
    e.yearlyPriceCents = d.getYearlyPriceCents();
    e.maxUsers = d.getMaxUsers();
    e.maxProjects = d.getMaxProjects();
    e.storageGb = d.getStorageGb();
    e.active = d.isActive();
    e.createdAt = d.getCreatedAt();
    e.updatedAt = d.getUpdatedAt();
    e.stripePriceIdMonthly = d.getStripePriceIdMonthly();
    e.stripePriceIdYearly = d.getStripePriceIdYearly();
    return e;
  }

  public PlanDefinition toDomain() {
    PlanDefinition p =
        new PlanDefinition(
            id,
            slug,
            displayName,
            description,
            monthlyPriceCents,
            yearlyPriceCents,
            maxUsers,
            maxProjects,
            storageGb,
            active,
            createdAt,
            updatedAt);
    p.setStripePriceIdMonthly(stripePriceIdMonthly);
    p.setStripePriceIdYearly(stripePriceIdYearly);
    return p;
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
}
