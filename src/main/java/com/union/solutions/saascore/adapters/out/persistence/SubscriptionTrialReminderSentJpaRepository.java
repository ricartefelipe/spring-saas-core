package com.union.solutions.saascore.adapters.out.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionTrialReminderSentJpaRepository
    extends JpaRepository<
        SubscriptionTrialReminderSentEntity, SubscriptionTrialReminderSentEntity.Pk> {

  boolean existsBySubscriptionIdAndReminderType(UUID subscriptionId, String reminderType);
}
