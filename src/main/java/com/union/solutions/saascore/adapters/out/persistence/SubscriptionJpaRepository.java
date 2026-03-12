package com.union.solutions.saascore.adapters.out.persistence;

import com.union.solutions.saascore.domain.Subscription.SubscriptionStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionJpaRepository extends JpaRepository<SubscriptionEntity, UUID> {

  Optional<SubscriptionEntity> findByTenantIdAndStatus(UUID tenantId, SubscriptionStatus status);
}
