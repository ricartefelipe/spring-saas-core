package com.union.solutions.saascore.adapters.out.persistence;

import com.union.solutions.saascore.application.port.SubscriptionTrialReminderSentRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionTrialReminderSentRepositoryAdapter
    implements SubscriptionTrialReminderSentRepository {

  private final SubscriptionTrialReminderSentJpaRepository jpa;

  public SubscriptionTrialReminderSentRepositoryAdapter(
      SubscriptionTrialReminderSentJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public boolean existsBySubscriptionIdAndReminderType(UUID subscriptionId, String reminderType) {
    return jpa.existsBySubscriptionIdAndReminderType(subscriptionId, reminderType);
  }

  @Override
  public void recordSent(UUID subscriptionId, String reminderType) {
    jpa.save(SubscriptionTrialReminderSentEntity.of(subscriptionId, reminderType));
  }
}
