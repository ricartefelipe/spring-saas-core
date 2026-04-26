package com.union.solutions.saascore.application.billing;

import com.union.solutions.saascore.application.port.OutboxPublisherPort;
import com.union.solutions.saascore.application.port.PlanDefinitionRepository;
import com.union.solutions.saascore.application.port.StripeBillingPort;
import com.union.solutions.saascore.application.port.SubscriptionRepository;
import com.union.solutions.saascore.application.port.TenantRepository;
import com.union.solutions.saascore.config.TenantContext;
import com.union.solutions.saascore.domain.PlanDefinition;
import com.union.solutions.saascore.domain.Subscription;
import com.union.solutions.saascore.domain.Subscription.SubscriptionStatus;
import com.union.solutions.saascore.domain.Tenant;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionUseCase {

  private static final Logger log = LoggerFactory.getLogger(SubscriptionUseCase.class);

  private final SubscriptionRepository subscriptionRepo;
  private final PlanDefinitionRepository planRepo;
  private final TenantRepository tenantRepo;
  private final OutboxPublisherPort outboxPublisher;
  private final StripeBillingPort billingPort;

  public SubscriptionUseCase(
      SubscriptionRepository subscriptionRepo,
      PlanDefinitionRepository planRepo,
      TenantRepository tenantRepo,
      OutboxPublisherPort outboxPublisher,
      StripeBillingPort billingPort) {
    this.subscriptionRepo = subscriptionRepo;
    this.planRepo = planRepo;
    this.tenantRepo = tenantRepo;
    this.outboxPublisher = outboxPublisher;
    this.billingPort = billingPort;
  }

  @Transactional
  public Subscription startTrial(UUID tenantId, String planCode) {
    PlanDefinition plan = requireActivePlan(planCode);

    subscriptionRepo
        .findCurrentByTenantId(tenantId)
        .ifPresent(
            existing -> {
              throw new IllegalStateException(
                  "Tenant already has an active subscription: " + existing.getStatus());
            });

    Instant now = Instant.now();
    Instant trialEnd = now.plus(Subscription.DEFAULT_TRIAL_DAYS, ChronoUnit.DAYS);

    Subscription subscription =
        new Subscription(
            UUID.randomUUID(),
            tenantId,
            planCode,
            SubscriptionStatus.TRIAL,
            now,
            trialEnd,
            trialEnd,
            null,
            null,
            null,
            now,
            now);

    Subscription saved = subscriptionRepo.save(subscription);
    syncTenantPlan(tenantId, planCode);

    outboxPublisher.publish(
        "SUBSCRIPTION",
        saved.getId().toString(),
        "subscription.trial_started",
        Map.of(
            "tenantId", tenantId.toString(),
            "planSlug", planCode,
            "trialEndsAt", trialEnd.toString()));

    log.info("Trial started for tenant={} plan={} trialEndsAt={}", tenantId, planCode, trialEnd);
    return saved;
  }

  @Transactional
  public Subscription activate(UUID tenantId) {
    Subscription sub =
        subscriptionRepo
            .findCurrentByTenantId(tenantId)
            .orElseThrow(
                () -> new IllegalStateException("No current subscription for tenant: " + tenantId));

    if (sub.getStatus() != SubscriptionStatus.TRIAL
        && sub.getStatus() != SubscriptionStatus.PAST_DUE) {
      throw new IllegalStateException("Cannot activate subscription in status: " + sub.getStatus());
    }

    Instant now = Instant.now();
    sub.setStatus(SubscriptionStatus.ACTIVE);
    sub.setCurrentPeriodStart(now);
    sub.setCurrentPeriodEnd(now.plus(30, ChronoUnit.DAYS));
    sub.setTrialEndsAt(null);
    sub.setGracePeriodEndsAt(null);
    sub.setUpdatedAt(now);

    Tenant tenant =
        tenantRepo
            .findById(tenantId)
            .orElseThrow(() -> new IllegalStateException("Tenant not found: " + tenantId));

    if (tenant.getStripeCustomerId() == null) {
      String customerId =
          billingPort.createCustomer(
              resolveBillingEmail(tenantId), tenant.getName(), tenantId.toString());
      tenant.setStripeCustomerId(customerId);
      tenant.setUpdatedAt(now);
      tenantRepo.save(tenant);
    }

    PlanDefinition plan =
        planRepo
            .findBySlug(sub.getPlanSlug())
            .orElseThrow(() -> new IllegalStateException("Plan not found: " + sub.getPlanSlug()));
    String priceId = plan.getStripePriceIdMonthly();
    if (priceId != null && !priceId.isBlank()) {
      String stripeSub = billingPort.createSubscription(tenant.getStripeCustomerId(), priceId);
      sub.setStripeSubscriptionId(stripeSub);
    }

    Subscription saved = subscriptionRepo.save(sub);

    outboxPublisher.publish(
        "SUBSCRIPTION",
        saved.getId().toString(),
        "subscription.activated",
        Map.of(
            "tenantId", tenantId.toString(),
            "planSlug", saved.getPlanSlug()));

    log.info("Subscription activated for tenant={} plan={}", tenantId, saved.getPlanSlug());
    return saved;
  }

  private String resolveBillingEmail(UUID tenantId) {
    String subject = TenantContext.getSubject();
    if (subject != null && subject.contains("@")) {
      return subject;
    }
    return "billing+" + tenantId + "@fluxe.local";
  }

  @Transactional
  public Subscription upgrade(UUID tenantId, String newPlanCode) {
    PlanDefinition newPlan = requireActivePlan(newPlanCode);

    Subscription sub =
        subscriptionRepo
            .findCurrentByTenantId(tenantId)
            .orElseThrow(
                () -> new IllegalStateException("No current subscription for tenant: " + tenantId));

    if (sub.getStatus() != SubscriptionStatus.ACTIVE) {
      throw new IllegalStateException(
          "Can only upgrade ACTIVE subscriptions, current: " + sub.getStatus());
    }

    PlanDefinition currentPlan =
        planRepo
            .findBySlug(sub.getPlanSlug())
            .orElseThrow(
                () -> new IllegalStateException("Current plan not found: " + sub.getPlanSlug()));

    if (newPlan.getMonthlyPriceCents() <= currentPlan.getMonthlyPriceCents()) {
      throw new IllegalArgumentException(
          "Upgrade requires a higher-tier plan. Use downgrade instead.");
    }

    if (sub.getStripeSubscriptionId() != null) {
      billingPort.cancelSubscription(sub.getStripeSubscriptionId());
    }
    String newStripePriceId = newPlan.getStripePriceIdMonthly();
    if (newStripePriceId != null && !newStripePriceId.isBlank()) {
      Tenant tenant =
          tenantRepo
              .findById(tenantId)
              .orElseThrow(() -> new IllegalStateException("Tenant not found: " + tenantId));
      if (tenant.getStripeCustomerId() != null) {
        String newStripeSub =
            billingPort.createSubscription(tenant.getStripeCustomerId(), newStripePriceId);
        sub.setStripeSubscriptionId(newStripeSub);
      }
    }

    String previousSlug = sub.getPlanSlug();
    Instant now = Instant.now();
    sub.setPreviousPlanSlug(previousSlug);
    sub.setPlanSlug(newPlanCode);
    sub.setUpdatedAt(now);

    Subscription saved = subscriptionRepo.save(sub);
    syncTenantPlan(tenantId, newPlanCode);

    outboxPublisher.publish(
        "SUBSCRIPTION",
        saved.getId().toString(),
        "subscription.upgraded",
        Map.of(
            "tenantId", tenantId.toString(),
            "previousPlan", previousSlug,
            "newPlan", newPlanCode));

    log.info(
        "Subscription upgraded for tenant={} from={} to={}", tenantId, previousSlug, newPlanCode);
    return saved;
  }

  @Transactional
  public Subscription downgrade(UUID tenantId, String newPlanCode) {
    PlanDefinition newPlan = requireActivePlan(newPlanCode);

    Subscription sub =
        subscriptionRepo
            .findCurrentByTenantId(tenantId)
            .orElseThrow(
                () -> new IllegalStateException("No current subscription for tenant: " + tenantId));

    if (sub.getStatus() != SubscriptionStatus.ACTIVE) {
      throw new IllegalStateException(
          "Can only downgrade ACTIVE subscriptions, current: " + sub.getStatus());
    }

    PlanDefinition currentPlan =
        planRepo
            .findBySlug(sub.getPlanSlug())
            .orElseThrow(
                () -> new IllegalStateException("Current plan not found: " + sub.getPlanSlug()));

    if (newPlan.getMonthlyPriceCents() >= currentPlan.getMonthlyPriceCents()) {
      throw new IllegalArgumentException(
          "Downgrade requires a lower-tier plan. Use upgrade instead.");
    }

    if (sub.getStripeSubscriptionId() != null) {
      billingPort.cancelSubscription(sub.getStripeSubscriptionId());
    }
    String newStripePriceId = newPlan.getStripePriceIdMonthly();
    if (newStripePriceId != null && !newStripePriceId.isBlank()) {
      Tenant tenant =
          tenantRepo
              .findById(tenantId)
              .orElseThrow(() -> new IllegalStateException("Tenant not found: " + tenantId));
      if (tenant.getStripeCustomerId() != null) {
        String newStripeSub =
            billingPort.createSubscription(tenant.getStripeCustomerId(), newStripePriceId);
        sub.setStripeSubscriptionId(newStripeSub);
      }
    }

    String previousSlug = sub.getPlanSlug();
    Instant now = Instant.now();
    sub.setPreviousPlanSlug(previousSlug);
    sub.setPlanSlug(newPlanCode);
    sub.setUpdatedAt(now);

    Subscription saved = subscriptionRepo.save(sub);
    syncTenantPlan(tenantId, newPlanCode);

    outboxPublisher.publish(
        "SUBSCRIPTION",
        saved.getId().toString(),
        "subscription.downgraded",
        Map.of(
            "tenantId", tenantId.toString(),
            "previousPlan", previousSlug,
            "newPlan", newPlanCode));

    log.info(
        "Subscription downgraded for tenant={} from={} to={}", tenantId, previousSlug, newPlanCode);
    return saved;
  }

  @Transactional
  public Subscription cancel(UUID tenantId) {
    Subscription sub =
        subscriptionRepo
            .findCurrentByTenantId(tenantId)
            .orElseThrow(
                () -> new IllegalStateException("No current subscription for tenant: " + tenantId));

    if (sub.getStripeSubscriptionId() != null) {
      billingPort.cancelSubscription(sub.getStripeSubscriptionId());
    }

    Instant now = Instant.now();
    sub.setStatus(SubscriptionStatus.CANCELLED);
    sub.setCancelledAt(now);
    sub.setUpdatedAt(now);

    Subscription saved = subscriptionRepo.save(sub);

    outboxPublisher.publish(
        "SUBSCRIPTION",
        saved.getId().toString(),
        "subscription.cancelled",
        Map.of(
            "tenantId", tenantId.toString(),
            "planSlug", saved.getPlanSlug(),
            "cancelledAt", now.toString()));

    log.info("Subscription cancelled for tenant={}", tenantId);
    return saved;
  }

  @Transactional
  public Subscription scheduleCancelAtPeriodEnd(UUID tenantId) {
    Subscription sub =
        subscriptionRepo
            .findCurrentByTenantId(tenantId)
            .orElseThrow(
                () -> new IllegalStateException("No current subscription for tenant: " + tenantId));
    if (sub.getStatus() != SubscriptionStatus.ACTIVE
        && sub.getStatus() != SubscriptionStatus.TRIAL) {
      throw new IllegalStateException(
          "Can only schedule cancel for ACTIVE or TRIAL subscription, current: " + sub.getStatus());
    }
    if (sub.getStripeSubscriptionId() != null) {
      billingPort.scheduleCancelAtPeriodEnd(sub.getStripeSubscriptionId());
    }
    Instant now = Instant.now();
    sub.setCancelAtPeriodEnd(true);
    sub.setUpdatedAt(now);
    Subscription saved = subscriptionRepo.save(sub);
    log.info("Subscription schedule cancel at period end for tenant={}", tenantId);
    return saved;
  }

  @Transactional
  public Subscription undoScheduleCancelAtPeriodEnd(UUID tenantId) {
    Subscription sub =
        subscriptionRepo
            .findCurrentByTenantId(tenantId)
            .orElseThrow(
                () -> new IllegalStateException("No current subscription for tenant: " + tenantId));
    if (!sub.isCancelAtPeriodEnd()) {
      return sub;
    }
    if (sub.getStripeSubscriptionId() != null) {
      billingPort.undoScheduleCancelAtPeriodEnd(sub.getStripeSubscriptionId());
    }
    Instant now = Instant.now();
    sub.setCancelAtPeriodEnd(false);
    sub.setUpdatedAt(now);
    Subscription saved = subscriptionRepo.save(sub);
    log.info("Subscription undo schedule cancel for tenant={}", tenantId);
    return saved;
  }

  @Transactional
  public Subscription reactivate(UUID tenantId) {
    Subscription sub =
        subscriptionRepo
            .findCurrentByTenantId(tenantId)
            .orElseGet(
                () ->
                    subscriptionRepo
                        .findActiveByTenantId(tenantId)
                        .orElseThrow(
                            () ->
                                new IllegalStateException(
                                    "No subscription found for tenant: " + tenantId)));

    if (sub.getStatus() != SubscriptionStatus.CANCELLED
        && sub.getStatus() != SubscriptionStatus.PAST_DUE) {
      throw new IllegalStateException(
          "Can only reactivate CANCELLED or PAST_DUE subscriptions, current: " + sub.getStatus());
    }

    Instant now = Instant.now();
    sub.setStatus(SubscriptionStatus.ACTIVE);
    sub.setCurrentPeriodStart(now);
    sub.setCurrentPeriodEnd(now.plus(30, ChronoUnit.DAYS));
    sub.setCancelledAt(null);
    sub.setGracePeriodEndsAt(null);
    sub.setUpdatedAt(now);

    Subscription saved = subscriptionRepo.save(sub);

    outboxPublisher.publish(
        "SUBSCRIPTION",
        saved.getId().toString(),
        "subscription.activated",
        Map.of(
            "tenantId", tenantId.toString(),
            "planSlug", saved.getPlanSlug(),
            "reactivated", "true"));

    log.info("Subscription reactivated for tenant={}", tenantId);
    return saved;
  }

  @Transactional
  public void processExpiredTrials() {
    Instant now = Instant.now();
    List<Subscription> expired = subscriptionRepo.findExpiredTrials(now);

    for (Subscription sub : expired) {
      sub.setStatus(SubscriptionStatus.EXPIRED);
      sub.setUpdatedAt(now);
      subscriptionRepo.save(sub);

      outboxPublisher.publish(
          "SUBSCRIPTION",
          sub.getId().toString(),
          "subscription.expired",
          Map.of(
              "tenantId", sub.getTenantId().toString(),
              "planSlug", sub.getPlanSlug()));

      log.info("Trial expired for subscription={} tenant={}", sub.getId(), sub.getTenantId());
    }

    if (!expired.isEmpty()) {
      log.info("Processed {} expired trials", expired.size());
    }
  }

  @Transactional
  public void processOverdueSubscriptions() {
    Instant now = Instant.now();
    List<Subscription> overdue = subscriptionRepo.findOverdueSubscriptions(now);

    for (Subscription sub : overdue) {
      sub.setStatus(SubscriptionStatus.CANCELLED);
      sub.setCancelledAt(now);
      sub.setUpdatedAt(now);
      subscriptionRepo.save(sub);

      outboxPublisher.publish(
          "SUBSCRIPTION",
          sub.getId().toString(),
          "subscription.cancelled",
          Map.of(
              "tenantId", sub.getTenantId().toString(),
              "planSlug", sub.getPlanSlug(),
              "reason", "grace_period_exceeded"));

      log.info(
          "Overdue subscription cancelled: subscription={} tenant={}",
          sub.getId(),
          sub.getTenantId());
    }

    if (!overdue.isEmpty()) {
      log.info("Processed {} overdue subscriptions", overdue.size());
    }
  }

  @Transactional(readOnly = true)
  public Subscription getCurrentSubscription(UUID tenantId) {
    return subscriptionRepo
        .findCurrentByTenantId(tenantId)
        .orElseThrow(
            () -> new IllegalStateException("No current subscription for tenant: " + tenantId));
  }

  private PlanDefinition requireActivePlan(String planSlug) {
    PlanDefinition plan =
        planRepo
            .findBySlug(planSlug)
            .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planSlug));

    if (!plan.isActive()) {
      throw new IllegalArgumentException("Plan is not active: " + planSlug);
    }
    return plan;
  }

  private void syncTenantPlan(UUID tenantId, String planSlug) {
    tenantRepo
        .findById(tenantId)
        .ifPresent(
            tenant -> {
              tenant.setPlan(planSlug);
              tenant.setUpdatedAt(Instant.now());
              tenantRepo.save(tenant);
            });
  }
}
