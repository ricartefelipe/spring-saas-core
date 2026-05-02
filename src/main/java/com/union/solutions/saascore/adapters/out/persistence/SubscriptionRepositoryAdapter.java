package com.union.solutions.saascore.adapters.out.persistence;

import com.union.solutions.saascore.application.port.SubscriptionRepository;
import com.union.solutions.saascore.domain.Subscription;
import com.union.solutions.saascore.domain.Subscription.SubscriptionStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionRepositoryAdapter implements SubscriptionRepository {

  private static final Set<SubscriptionStatus> CURRENT_STATUSES =
      Set.of(SubscriptionStatus.TRIAL, SubscriptionStatus.ACTIVE, SubscriptionStatus.PAST_DUE);

  private final SubscriptionJpaRepository jpa;

  public SubscriptionRepositoryAdapter(SubscriptionJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public Subscription save(Subscription subscription) {
    return jpa.save(SubscriptionEntity.fromDomain(subscription)).toDomain();
  }

  @Override
  public Optional<Subscription> findById(UUID id) {
    return jpa.findById(id).map(SubscriptionEntity::toDomain);
  }

  @Override
  public Optional<Subscription> findActiveByTenantId(UUID tenantId) {
    return jpa.findByTenantIdAndStatus(tenantId, SubscriptionStatus.ACTIVE)
        .map(SubscriptionEntity::toDomain);
  }

  @Override
  public Optional<Subscription> findCurrentByTenantId(UUID tenantId) {
    return jpa.findByTenantIdAndStatusIn(tenantId, CURRENT_STATUSES)
        .map(SubscriptionEntity::toDomain);
  }

  @Override
  public List<Subscription> findExpiredTrials(Instant cutoff) {
    return jpa.findByStatusAndTrialEndsAtBefore(SubscriptionStatus.TRIAL, cutoff).stream()
        .map(SubscriptionEntity::toDomain)
        .toList();
  }

  @Override
  public List<Subscription> findTrialsWithTrialEndingBetween(
      Instant startInclusive, Instant endExclusive) {
    return jpa
        .findByStatusAndTrialEndsAtGreaterThanEqualAndTrialEndsAtLessThan(
            SubscriptionStatus.TRIAL, startInclusive, endExclusive)
        .stream()
        .map(SubscriptionEntity::toDomain)
        .toList();
  }

  @Override
  public List<Subscription> findOverdueSubscriptions(Instant cutoff) {
    return jpa.findByStatusAndGracePeriodEndsAtBefore(SubscriptionStatus.PAST_DUE, cutoff).stream()
        .map(SubscriptionEntity::toDomain)
        .toList();
  }
}
