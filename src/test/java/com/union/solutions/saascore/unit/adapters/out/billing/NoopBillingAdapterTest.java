package com.union.solutions.saascore.unit.adapters.out.billing;

import static org.assertj.core.api.Assertions.assertThat;

import com.union.solutions.saascore.adapters.out.billing.NoopBillingAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NoopBillingAdapterTest {

  private NoopBillingAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new NoopBillingAdapter();
  }

  @Test
  void createCustomer_returnsFakeIdWithPrefix() {
    String result = adapter.createCustomer("a@b.com", "Foo", "tid-123");
    assertThat(result).startsWith("cus_noop_").hasSize(17);
  }

  @Test
  void createSubscription_returnsFakeIdWithPrefix() {
    String result = adapter.createSubscription("cus_123", "price_456");
    assertThat(result).startsWith("sub_noop_").hasSize(17);
  }

  @Test
  void cancelSubscription_doesNotThrow() {
    adapter.cancelSubscription("sub_123");
  }

  @Test
  void createBillingPortalSession_returnsFakeUrl() {
    String result = adapter.createBillingPortalSession("cus_123", "https://app.com/billing");
    assertThat(result).isEqualTo("https://billing.stripe.com/noop-session");
  }

  @Test
  void listSubscriptionInvoices_returnsEmptyList() {
    assertThat(adapter.listSubscriptionInvoices("sub_123")).isEmpty();
  }
}
