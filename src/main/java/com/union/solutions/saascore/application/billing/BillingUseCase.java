package com.union.solutions.saascore.application.billing;

import com.union.solutions.saascore.application.port.PlanDefinitionRepository;
import com.union.solutions.saascore.application.port.SubscriptionRepository;
import com.union.solutions.saascore.application.port.TenantRepository;
import com.union.solutions.saascore.domain.PlanDefinition;
import com.union.solutions.saascore.domain.Subscription;
import com.union.solutions.saascore.domain.Subscription.SubscriptionStatus;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingUseCase {

  private final PlanDefinitionRepository planRepo;
  private final SubscriptionRepository subscriptionRepo;
  private final TenantRepository tenantRepo;

  public BillingUseCase(
      PlanDefinitionRepository planRepo,
      SubscriptionRepository subscriptionRepo,
      TenantRepository tenantRepo) {
    this.planRepo = planRepo;
    this.subscriptionRepo = subscriptionRepo;
    this.tenantRepo = tenantRepo;
  }

  @Transactional(readOnly = true)
  public List<PlanDefinition> listPlans() {
    return planRepo.findAllActive();
  }

  @Transactional(readOnly = true)
  public Optional<PlanDefinition> getPlan(String slug) {
    return planRepo.findBySlug(slug);
  }

  @Transactional
  public Subscription createSubscription(UUID tenantId, String planSlug) {
    PlanDefinition plan =
        planRepo
            .findBySlug(planSlug)
            .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planSlug));

    if (!plan.isActive()) {
      throw new IllegalArgumentException("Plan is not active: " + planSlug);
    }

    subscriptionRepo
        .findCurrentByTenantId(tenantId)
        .ifPresent(
            existing -> {
              existing.setStatus(SubscriptionStatus.CANCELLED);
              existing.setCancelledAt(Instant.now());
              existing.setUpdatedAt(Instant.now());
              subscriptionRepo.save(existing);
            });

    Instant now = Instant.now();
    Subscription subscription =
        new Subscription(
            UUID.randomUUID(),
            tenantId,
            planSlug,
            SubscriptionStatus.ACTIVE,
            now,
            now.plus(30, ChronoUnit.DAYS),
            null,
            null,
            null,
            null,
            now,
            now);

    Subscription saved = subscriptionRepo.save(subscription);

    tenantRepo
        .findById(tenantId)
        .ifPresent(
            tenant -> {
              tenant.setPlan(planSlug);
              tenant.setUpdatedAt(Instant.now());
              tenantRepo.save(tenant);
            });

    return saved;
  }

  @Transactional
  public Optional<Subscription> cancelSubscription(UUID tenantId) {
    return subscriptionRepo
        .findCurrentByTenantId(tenantId)
        .map(
            sub -> {
              sub.setStatus(SubscriptionStatus.CANCELLED);
              sub.setCancelledAt(Instant.now());
              sub.setUpdatedAt(Instant.now());
              return subscriptionRepo.save(sub);
            });
  }

  @Transactional(readOnly = true)
  public Optional<Subscription> getSubscription(UUID tenantId) {
    return subscriptionRepo.findCurrentByTenantId(tenantId);
  }
}
