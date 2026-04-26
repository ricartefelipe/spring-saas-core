package com.union.solutions.saascore.unit.application.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.union.solutions.saascore.application.billing.BillingUseCase;
import com.union.solutions.saascore.application.port.PlanDefinitionRepository;
import com.union.solutions.saascore.application.port.SubscriptionRepository;
import com.union.solutions.saascore.application.port.TenantRepository;
import com.union.solutions.saascore.domain.Subscription;
import com.union.solutions.saascore.domain.Subscription.SubscriptionStatus;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BillingUseCaseTest {

  @Mock PlanDefinitionRepository planRepo;
  @Mock SubscriptionRepository subscriptionRepo;
  @Mock TenantRepository tenantRepo;

  @Test
  void getSubscription_returnsCurrentTrialSubscription() {
    UUID tenantId = UUID.randomUUID();
    Subscription trial = new Subscription();
    trial.setTenantId(tenantId);
    trial.setStatus(SubscriptionStatus.TRIAL);
    when(subscriptionRepo.findCurrentByTenantId(tenantId)).thenReturn(Optional.of(trial));

    BillingUseCase useCase = new BillingUseCase(planRepo, subscriptionRepo, tenantRepo);

    assertThat(useCase.getSubscription(tenantId)).containsSame(trial);
  }
}
