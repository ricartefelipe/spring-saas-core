package com.union.solutions.saascore.application.port;

public interface StripeBillingPort {

  String createCustomer(String email, String name, String tenantId);

  String createSubscription(String customerId, String priceId);

  void cancelSubscription(String subscriptionId);

  String createBillingPortalSession(String customerId, String returnUrl);
}
