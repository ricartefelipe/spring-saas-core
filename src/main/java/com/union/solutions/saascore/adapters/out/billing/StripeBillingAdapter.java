package com.union.solutions.saascore.adapters.out.billing;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Subscription;
import com.stripe.model.billingportal.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.SubscriptionCancelParams;
import com.stripe.param.SubscriptionCreateParams;
import com.stripe.param.billingportal.SessionCreateParams;
import com.union.solutions.saascore.application.port.StripeBillingPort;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.billing.provider", havingValue = "stripe")
public class StripeBillingAdapter implements StripeBillingPort {

  private final String secretKey;

  public StripeBillingAdapter(@Value("${app.billing.stripe-secret-key:}") String secretKey) {
    this.secretKey = secretKey;
  }

  @PostConstruct
  void init() {
    Stripe.apiKey = secretKey;
  }

  @Override
  public String createCustomer(String email, String name, String tenantId) {
    try {
      CustomerCreateParams params =
          CustomerCreateParams.builder()
              .setEmail(email)
              .setName(name)
              .putMetadata("tenantId", tenantId)
              .build();
      Customer customer = Customer.create(params);
      return customer.getId();
    } catch (StripeException e) {
      throw new BillingException("Failed to create Stripe customer", e);
    }
  }

  @Override
  public String createSubscription(String customerId, String priceId) {
    try {
      SubscriptionCreateParams params =
          SubscriptionCreateParams.builder()
              .setCustomer(customerId)
              .addItem(SubscriptionCreateParams.Item.builder().setPrice(priceId).build())
              .build();
      Subscription subscription = Subscription.create(params);
      return subscription.getId();
    } catch (StripeException e) {
      throw new BillingException("Failed to create Stripe subscription", e);
    }
  }

  @Override
  public void cancelSubscription(String subscriptionId) {
    try {
      Subscription subscription = Subscription.retrieve(subscriptionId);
      subscription.cancel(SubscriptionCancelParams.builder().build());
    } catch (StripeException e) {
      throw new BillingException("Failed to cancel Stripe subscription", e);
    }
  }

  @Override
  public String createBillingPortalSession(String customerId, String returnUrl) {
    try {
      SessionCreateParams params =
          SessionCreateParams.builder().setCustomer(customerId).setReturnUrl(returnUrl).build();
      Session session = Session.create(params);
      return session.getUrl();
    } catch (StripeException e) {
      throw new BillingException("Failed to create billing portal session", e);
    }
  }
}
