package com.union.solutions.saascore.application.port;

import com.union.solutions.saascore.domain.Subscription;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository {

  Subscription save(Subscription subscription);

  Optional<Subscription> findById(UUID id);

  Optional<Subscription> findActiveByTenantId(UUID tenantId);
}
