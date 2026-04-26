package com.union.solutions.saascore.application.port;

import java.util.List;

public interface StripeBillingPort {

  String createCustomer(String email, String name, String tenantId);

  String createSubscription(String customerId, String priceId);

  void cancelSubscription(String subscriptionId);

  /** Schedule cancellation at period end (Stripe cancel_at_period_end = true). */
  void scheduleCancelAtPeriodEnd(String subscriptionId);

  /** Undo schedule: cancel_at_period_end = false. */
  void undoScheduleCancelAtPeriodEnd(String subscriptionId);

  String createBillingPortalSession(String customerId, String returnUrl);

  List<BillingInvoice> listSubscriptionInvoices(String subscriptionId);
}
