package com.union.solutions.saascore.unit.adapters.in.rest;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.union.solutions.saascore.adapters.in.rest.BillingController;
import com.union.solutions.saascore.application.abac.AbacEvaluator;
import com.union.solutions.saascore.application.billing.BillingUseCase;
import com.union.solutions.saascore.application.port.BillingInvoice;
import com.union.solutions.saascore.application.port.StripeBillingPort;
import com.union.solutions.saascore.application.port.SubscriptionRepository;
import com.union.solutions.saascore.application.port.TenantRepository;
import com.union.solutions.saascore.config.TenantContext;
import com.union.solutions.saascore.domain.Subscription;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class BillingControllerTest {

  @Mock BillingUseCase billingUseCase;
  @Mock StripeBillingPort billingPort;
  @Mock TenantRepository tenantRepo;
  @Mock SubscriptionRepository subscriptionRepo;
  @Mock AbacEvaluator abacEvaluator;

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Test
  void listInvoices_usesCurrentSubscriptionFromTenantContext() throws Exception {
    UUID tenantId = UUID.randomUUID();
    TenantContext.setTenantId(tenantId);
    Subscription subscription = subscription(tenantId, "sub_stripe_123");
    when(subscriptionRepo.findCurrentByTenantId(tenantId)).thenReturn(Optional.of(subscription));
    when(billingPort.listSubscriptionInvoices("sub_stripe_123"))
        .thenReturn(
            List.of(
                new BillingInvoice(
                    "in_123",
                    "paid",
                    "BRL",
                    1000,
                    Instant.parse("2026-04-01T00:00:00Z"),
                    Instant.parse("2026-04-01T00:00:00Z"),
                    Instant.parse("2026-05-01T00:00:00Z"),
                    "https://billing.example/in_123",
                    "https://billing.example/in_123.pdf")));

    mvc()
        .perform(get("/v1/billing/invoices"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value("in_123"))
        .andExpect(jsonPath("$[0].status").value("paid"))
        .andExpect(jsonPath("$[0].amountDueCents").value(1000))
        .andExpect(jsonPath("$[0].invoicePdfUrl").value("https://billing.example/in_123.pdf"));

    verify(abacEvaluator).enforceOrThrow("profile:read");
    verify(billingPort).listSubscriptionInvoices(eq("sub_stripe_123"));
  }

  @Test
  void listInvoices_withoutStripeSubscription_returnsEmptyList() throws Exception {
    UUID tenantId = UUID.randomUUID();
    TenantContext.setTenantId(tenantId);
    when(subscriptionRepo.findCurrentByTenantId(tenantId))
        .thenReturn(Optional.of(subscription(tenantId, null)));

    mvc()
        .perform(get("/v1/billing/invoices"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());
  }

  private MockMvc mvc() {
    return MockMvcBuilders.standaloneSetup(
            new BillingController(
                billingUseCase, billingPort, tenantRepo, subscriptionRepo, abacEvaluator))
        .build();
  }

  private Subscription subscription(UUID tenantId, String stripeSubscriptionId) {
    Subscription subscription =
        new Subscription(
            UUID.randomUUID(),
            tenantId,
            "pro",
            Subscription.SubscriptionStatus.ACTIVE,
            Instant.parse("2026-04-01T00:00:00Z"),
            Instant.parse("2026-05-01T00:00:00Z"),
            null,
            null,
            null,
            null,
            Instant.parse("2026-04-01T00:00:00Z"),
            Instant.parse("2026-04-01T00:00:00Z"));
    subscription.setStripeSubscriptionId(stripeSubscriptionId);
    return subscription;
  }
}
