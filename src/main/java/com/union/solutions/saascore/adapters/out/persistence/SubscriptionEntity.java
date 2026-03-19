package com.union.solutions.saascore.adapters.out.persistence;

import com.union.solutions.saascore.domain.Subscription;
import com.union.solutions.saascore.domain.Subscription.SubscriptionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
public class SubscriptionEntity {

  @Id private UUID id;

  @Column(name = "tenant_id", nullable = false)
  private UUID tenantId;

  @Column(name = "plan_slug", nullable = false, length = 50)
  private String planSlug;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private SubscriptionStatus status;

  @Column(name = "current_period_start", nullable = false)
  private Instant currentPeriodStart;

  @Column(name = "current_period_end", nullable = false)
  private Instant currentPeriodEnd;

  @Column(name = "trial_ends_at")
  private Instant trialEndsAt;

  @Column(name = "grace_period_ends_at")
  private Instant gracePeriodEndsAt;

  @Column(name = "previous_plan_slug", length = 50)
  private String previousPlanSlug;

  @Column(name = "cancelled_at")
  private Instant cancelledAt;

  @Column(name = "cancel_at_period_end", nullable = false)
  private boolean cancelAtPeriodEnd;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "stripe_subscription_id")
  private String stripeSubscriptionId;

  public static SubscriptionEntity fromDomain(Subscription d) {
    SubscriptionEntity e = new SubscriptionEntity();
    e.id = d.getId();
    e.tenantId = d.getTenantId();
    e.planSlug = d.getPlanSlug();
    e.status = d.getStatus();
    e.currentPeriodStart = d.getCurrentPeriodStart();
    e.currentPeriodEnd = d.getCurrentPeriodEnd();
    e.trialEndsAt = d.getTrialEndsAt();
    e.gracePeriodEndsAt = d.getGracePeriodEndsAt();
    e.previousPlanSlug = d.getPreviousPlanSlug();
    e.cancelledAt = d.getCancelledAt();
    e.cancelAtPeriodEnd = d.isCancelAtPeriodEnd();
    e.createdAt = d.getCreatedAt();
    e.updatedAt = d.getUpdatedAt();
    e.stripeSubscriptionId = d.getStripeSubscriptionId();
    return e;
  }

  public Subscription toDomain() {
    Subscription s =
        new Subscription(
            id,
            tenantId,
            planSlug,
            status,
            currentPeriodStart,
            currentPeriodEnd,
            trialEndsAt,
            gracePeriodEndsAt,
            previousPlanSlug,
            cancelledAt,
            createdAt,
            updatedAt);
    s.setStripeSubscriptionId(stripeSubscriptionId);
    s.setCancelAtPeriodEnd(cancelAtPeriodEnd);
    return s;
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
}
