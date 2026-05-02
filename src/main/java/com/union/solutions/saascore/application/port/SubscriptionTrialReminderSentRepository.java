package com.union.solutions.saascore.application.port;

import java.util.UUID;

public interface SubscriptionTrialReminderSentRepository {

  boolean existsBySubscriptionIdAndReminderType(UUID subscriptionId, String reminderType);

  void recordSent(UUID subscriptionId, String reminderType);
}
