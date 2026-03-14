package com.union.solutions.saascore.adapters.out.billing;

import com.union.solutions.saascore.application.port.StripeBillingPort;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.billing.provider", havingValue = "noop", matchIfMissing = true)
public class NoopBillingAdapter implements StripeBillingPort {

  private static final Logger log = LoggerFactory.getLogger(NoopBillingAdapter.class);

  @Override
  public String createCustomer(String email, String name, String tenantId) {
    String fakeId = "cus_noop_" + UUID.randomUUID().toString().substring(0, 8);
    log.info(
        "NOOP createCustomer email={} name={} tenantId={} -> {}", email, name, tenantId, fakeId);
    return fakeId;
  }

  @Override
  public String createSubscription(String customerId, String priceId) {
    String fakeId = "sub_noop_" + UUID.randomUUID().toString().substring(0, 8);
    log.info("NOOP createSubscription customerId={} priceId={} -> {}", customerId, priceId, fakeId);
    return fakeId;
  }

  @Override
  public void cancelSubscription(String subscriptionId) {
    log.info("NOOP cancelSubscription subscriptionId={}", subscriptionId);
  }

  @Override
  public String createBillingPortalSession(String customerId, String returnUrl) {
    String fakeUrl = "https://billing.stripe.com/noop-session";
    log.info(
        "NOOP createBillingPortalSession customerId={} returnUrl={} -> {}",
        customerId,
        returnUrl,
        fakeUrl);
    return fakeUrl;
  }
}
