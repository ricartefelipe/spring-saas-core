package com.union.solutions.saascore.unit.application.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.union.solutions.saascore.application.billing.SubscriptionUseCase;
import com.union.solutions.saascore.application.port.OutboxPublisherPort;
import com.union.solutions.saascore.application.port.PlanDefinitionRepository;
import com.union.solutions.saascore.application.port.StripeBillingPort;
import com.union.solutions.saascore.application.port.SubscriptionRepository;
import com.union.solutions.saascore.application.port.TenantRepository;
import com.union.solutions.saascore.domain.Subscription;
import com.union.solutions.saascore.domain.Subscription.SubscriptionStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionUseCaseScheduleCancelTest {

  @Mock SubscriptionRepository subscriptionRepo;
  @Mock PlanDefinitionRepository planRepo;
  @Mock TenantRepository tenantRepo;
  @Mock OutboxPublisherPort outboxPublisher;
  @Mock StripeBillingPort billingPort;

  private SubscriptionUseCase useCase;
  private UUID tenantId;
  private Subscription activeSub;

  @BeforeEach
  void setUp() {
    useCase =
        new SubscriptionUseCase(
            subscriptionRepo, planRepo, tenantRepo, outboxPublisher, billingPort);
    tenantId = UUID.randomUUID();
    Instant now = Instant.now();
    activeSub = new Subscription();
    activeSub.setId(UUID.randomUUID());
    activeSub.setTenantId(tenantId);
    activeSub.setPlanSlug("pro");
    activeSub.setStatus(SubscriptionStatus.ACTIVE);
    activeSub.setCurrentPeriodStart(now);
    activeSub.setCurrentPeriodEnd(now.plusSeconds(86400));
    activeSub.setCancelAtPeriodEnd(false);
    activeSub.setStripeSubscriptionId("sub_stripe_123");
    activeSub.setCreatedAt(now);
    activeSub.setUpdatedAt(now);
  }

  @Test
  void scheduleCancelAtPeriodEnd_setsFlagAndCallsStripe_whenActive() {
    when(subscriptionRepo.findCurrentByTenantId(tenantId)).thenReturn(Optional.of(activeSub));
    when(subscriptionRepo.save(any(Subscription.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Subscription saved = useCase.scheduleCancelAtPeriodEnd(tenantId);

    verify(billingPort).scheduleCancelAtPeriodEnd("sub_stripe_123");
    assertThat(saved.isCancelAtPeriodEnd()).isTrue();
    ArgumentCaptor<Subscription> cap = ArgumentCaptor.forClass(Subscription.class);
    verify(subscriptionRepo).save(cap.capture());
    assertThat(cap.getValue().isCancelAtPeriodEnd()).isTrue();
  }

  @Test
  void scheduleCancelAtPeriodEnd_setsFlagWithoutStripeCall_whenNoStripeSubscription() {
    activeSub.setStripeSubscriptionId(null);
    when(subscriptionRepo.findCurrentByTenantId(tenantId)).thenReturn(Optional.of(activeSub));
    when(subscriptionRepo.save(any(Subscription.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Subscription saved = useCase.scheduleCancelAtPeriodEnd(tenantId);

    verify(billingPort, never()).scheduleCancelAtPeriodEnd(any());
    assertThat(saved.isCancelAtPeriodEnd()).isTrue();
  }

  @Test
  void scheduleCancelAtPeriodEnd_throws_whenCancelled() {
    activeSub.setStatus(SubscriptionStatus.CANCELLED);
    when(subscriptionRepo.findCurrentByTenantId(tenantId)).thenReturn(Optional.of(activeSub));

    assertThatThrownBy(() -> useCase.scheduleCancelAtPeriodEnd(tenantId))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("ACTIVE or TRIAL");

    verify(billingPort, never()).scheduleCancelAtPeriodEnd(any());
    verify(subscriptionRepo, never()).save(any());
  }

  @Test
  void undoScheduleCancelAtPeriodEnd_callsStripeAndClearsFlag_whenWasScheduled() {
    activeSub.setCancelAtPeriodEnd(true);
    when(subscriptionRepo.findCurrentByTenantId(tenantId)).thenReturn(Optional.of(activeSub));
    when(subscriptionRepo.save(any(Subscription.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Subscription saved = useCase.undoScheduleCancelAtPeriodEnd(tenantId);

    verify(billingPort).undoScheduleCancelAtPeriodEnd(eq("sub_stripe_123"));
    assertThat(saved.isCancelAtPeriodEnd()).isFalse();
  }

  @Test
  void undoScheduleCancelAtPeriodEnd_returnsUnchanged_whenNotScheduled() {
    when(subscriptionRepo.findCurrentByTenantId(tenantId)).thenReturn(Optional.of(activeSub));

    Subscription saved = useCase.undoScheduleCancelAtPeriodEnd(tenantId);

    verify(billingPort, never()).undoScheduleCancelAtPeriodEnd(any());
    verify(subscriptionRepo, never()).save(any());
    assertThat(saved).isSameAs(activeSub);
  }
}
