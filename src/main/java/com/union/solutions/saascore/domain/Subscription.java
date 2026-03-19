package com.union.solutions.saascore.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Subscription {

  public static final int DEFAULT_TRIAL_DAYS = 14;
  public static final int GRACE_PERIOD_DAYS = 7;

  private UUID id;
  private UUID tenantId;
  private String planSlug;
  private SubscriptionStatus status;
  private Instant currentPeriodStart;
  private Instant currentPeriodEnd;
  private Instant trialEndsAt;
  private Instant gracePeriodEndsAt;
  private String previousPlanSlug;
  private Instant cancelledAt;
  private boolean cancelAtPeriodEnd;
  private Instant createdAt;
  private Instant updatedAt;
  private String stripeSubscriptionId;

  public Subscription() {}

  public Subscription(
      UUID id,
      UUID tenantId,
      String planSlug,
      SubscriptionStatus status,
      Instant currentPeriodStart,
      Instant currentPeriodEnd,
      Instant trialEndsAt,
      Instant gracePeriodEndsAt,
      String previousPlanSlug,
      Instant cancelledAt,
      Instant createdAt,
      Instant updatedAt) {
    this.id = id;
    this.tenantId = tenantId;
    this.planSlug = planSlug;
    this.status = status;
    this.currentPeriodStart = currentPeriodStart;
    this.currentPeriodEnd = currentPeriodEnd;
    this.trialEndsAt = trialEndsAt;
    this.gracePeriodEndsAt = gracePeriodEndsAt;
    this.previousPlanSlug = previousPlanSlug;
    this.cancelledAt = cancelledAt;
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

  public String getPlanSlug() {
    return planSlug;
  }

  public void setPlanSlug(String planSlug) {
    this.planSlug = planSlug;
  }

  public SubscriptionStatus getStatus() {
    return status;
  }

  public void setStatus(SubscriptionStatus status) {
    this.status = status;
  }

  public Instant getCurrentPeriodStart() {
    return currentPeriodStart;
  }

  public void setCurrentPeriodStart(Instant currentPeriodStart) {
    this.currentPeriodStart = currentPeriodStart;
  }

  public Instant getCurrentPeriodEnd() {
    return currentPeriodEnd;
  }

  public void setCurrentPeriodEnd(Instant currentPeriodEnd) {
    this.currentPeriodEnd = currentPeriodEnd;
  }

  public Instant getTrialEndsAt() {
    return trialEndsAt;
  }

  public void setTrialEndsAt(Instant trialEndsAt) {
    this.trialEndsAt = trialEndsAt;
  }

  public Instant getGracePeriodEndsAt() {
    return gracePeriodEndsAt;
  }

  public void setGracePeriodEndsAt(Instant gracePeriodEndsAt) {
    this.gracePeriodEndsAt = gracePeriodEndsAt;
  }

  public String getPreviousPlanSlug() {
    return previousPlanSlug;
  }

  public void setPreviousPlanSlug(String previousPlanSlug) {
    this.previousPlanSlug = previousPlanSlug;
  }

  public Instant getCancelledAt() {
    return cancelledAt;
  }

  public void setCancelledAt(Instant cancelledAt) {
    this.cancelledAt = cancelledAt;
  }

  public boolean isCancelAtPeriodEnd() {
    return cancelAtPeriodEnd;
  }

  public void setCancelAtPeriodEnd(boolean cancelAtPeriodEnd) {
    this.cancelAtPeriodEnd = cancelAtPeriodEnd;
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

  public String getStripeSubscriptionId() {
    return stripeSubscriptionId;
  }

  public void setStripeSubscriptionId(String stripeSubscriptionId) {
    this.stripeSubscriptionId = stripeSubscriptionId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Subscription that)) return false;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  public enum SubscriptionStatus {
    TRIAL,
    ACTIVE,
    PAST_DUE,
    CANCELLED,
    EXPIRED
  }
}
