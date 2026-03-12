package com.union.solutions.saascore.infrastructure.billing;

import com.union.solutions.saascore.application.billing.SubscriptionUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionScheduler {

  private static final Logger log = LoggerFactory.getLogger(SubscriptionScheduler.class);

  private final SubscriptionUseCase subscriptionUseCase;

  public SubscriptionScheduler(SubscriptionUseCase subscriptionUseCase) {
    this.subscriptionUseCase = subscriptionUseCase;
  }

  @Scheduled(cron = "0 0 3 * * *")
  public void processTrialsAndOverdue() {
    log.info("Starting scheduled subscription lifecycle processing");
    try {
      subscriptionUseCase.processExpiredTrials();
    } catch (Exception e) {
      log.error("Error processing expired trials", e);
    }
    try {
      subscriptionUseCase.processOverdueSubscriptions();
    } catch (Exception e) {
      log.error("Error processing overdue subscriptions", e);
    }
    log.info("Finished scheduled subscription lifecycle processing");
  }
}
