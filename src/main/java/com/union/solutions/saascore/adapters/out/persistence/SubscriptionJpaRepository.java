package com.union.solutions.saascore.adapters.out.persistence;

import com.union.solutions.saascore.domain.Subscription.SubscriptionStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionJpaRepository extends JpaRepository<SubscriptionEntity, UUID> {

  Optional<SubscriptionEntity> findByTenantIdAndStatus(UUID tenantId, SubscriptionStatus status);

  Optional<SubscriptionEntity> findByTenantIdAndStatusIn(
      UUID tenantId, Collection<SubscriptionStatus> statuses);

  List<SubscriptionEntity> findByStatusAndTrialEndsAtBefore(
      SubscriptionStatus status, Instant cutoff);

  List<SubscriptionEntity> findByStatusAndGracePeriodEndsAtBefore(
      SubscriptionStatus status, Instant cutoff);
}
