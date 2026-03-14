package com.union.solutions.saascore.adapters.out.persistence;

import com.union.solutions.saascore.domain.Tenant;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenants")
public class TenantEntity {

  @Id
  @Column(name = "id")
  private UUID id;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "plan", nullable = false, length = 64)
  private String plan;

  @Column(name = "region", nullable = false, length = 64)
  private String region;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private Tenant.TenantStatus status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "stripe_customer_id")
  private String stripeCustomerId;

  public static TenantEntity from(Tenant t) {
    TenantEntity e = new TenantEntity();
    e.id = t.getId();
    e.name = t.getName();
    e.plan = t.getPlan();
    e.region = t.getRegion();
    e.status = t.getStatus();
    e.createdAt = t.getCreatedAt();
    e.updatedAt = t.getUpdatedAt();
    e.stripeCustomerId = t.getStripeCustomerId();
    return e;
  }

  public Tenant toDomain() {
    Tenant t = new Tenant(id, name, plan, region, status, createdAt, updatedAt);
    t.setStripeCustomerId(stripeCustomerId);
    return t;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPlan() {
    return plan;
  }

  public void setPlan(String plan) {
    this.plan = plan;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public Tenant.TenantStatus getStatus() {
    return status;
  }

  public void setStatus(Tenant.TenantStatus status) {
    this.status = status;
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

  public String getStripeCustomerId() {
    return stripeCustomerId;
  }

  public void setStripeCustomerId(String stripeCustomerId) {
    this.stripeCustomerId = stripeCustomerId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof TenantEntity that)) return false;
    return id != null && java.util.Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
