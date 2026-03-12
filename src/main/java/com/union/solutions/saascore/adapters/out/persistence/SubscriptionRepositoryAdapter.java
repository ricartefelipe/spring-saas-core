package com.union.solutions.saascore.adapters.out.persistence;

import com.union.solutions.saascore.application.port.SubscriptionRepository;
import com.union.solutions.saascore.domain.Subscription;
import com.union.solutions.saascore.domain.Subscription.SubscriptionStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionRepositoryAdapter implements SubscriptionRepository {

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
}
