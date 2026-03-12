package com.union.solutions.saascore.application.port;

import com.union.solutions.saascore.domain.Subscription;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository {

  Subscription save(Subscription subscription);

  Optional<Subscription> findById(UUID id);

  Optional<Subscription> findActiveByTenantId(UUID tenantId);

  Optional<Subscription> findCurrentByTenantId(UUID tenantId);

  List<Subscription> findExpiredTrials(Instant cutoff);

  List<Subscription> findOverdueSubscriptions(Instant cutoff);
}
