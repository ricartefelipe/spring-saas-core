package com.union.solutions.saascore.unit.adapters.in.rest;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.union.solutions.saascore.adapters.in.rest.BusinessMetricsController;
import com.union.solutions.saascore.application.port.TenantRepository;
import com.union.solutions.saascore.application.service.FeatureFlagService;
import com.union.solutions.saascore.application.service.PolicyService;
import com.union.solutions.saascore.domain.Tenant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class BusinessMetricsControllerTest {

  @Mock TenantRepository tenantRepo;
  @Mock PolicyService policyService;
  @Mock FeatureFlagService flagService;

  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    mvc =
        MockMvcBuilders.standaloneSetup(
                new BusinessMetricsController(tenantRepo, policyService, flagService))
            .build();
  }

  @Test
  void metrics_returnsAggregatedCounts() throws Exception {
    when(tenantRepo.countByStatus(Tenant.TenantStatus.ACTIVE)).thenReturn(10L);
    when(tenantRepo.countByStatus(Tenant.TenantStatus.SUSPENDED)).thenReturn(2L);
    when(tenantRepo.countByStatus(Tenant.TenantStatus.DELETED)).thenReturn(1L);
    when(tenantRepo.countByPlanAndStatus())
        .thenReturn(
            List.of(
                new TenantRepository.PlanStatusCount("free", "ACTIVE", 5),
                new TenantRepository.PlanStatusCount("pro", "ACTIVE", 5)));
    when(policyService.countActive()).thenReturn(8L);
    when(flagService.countActiveFlags()).thenReturn(3L);

    mvc.perform(get("/v1/metrics/business"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tenants.active").value(10))
        .andExpect(jsonPath("$.tenants.suspended").value(2))
        .andExpect(jsonPath("$.tenants.deleted").value(1))
        .andExpect(jsonPath("$.tenants.total").value(13))
        .andExpect(jsonPath("$.tenants_by_plan.free.ACTIVE").value(5))
        .andExpect(jsonPath("$.tenants_by_plan.pro.ACTIVE").value(5))
        .andExpect(jsonPath("$.active_policies").value(8))
        .andExpect(jsonPath("$.active_flags").value(3));
  }

  @Test
  void metrics_withZeroCounts_returnsZeros() throws Exception {
    when(tenantRepo.countByStatus(Tenant.TenantStatus.ACTIVE)).thenReturn(0L);
    when(tenantRepo.countByStatus(Tenant.TenantStatus.SUSPENDED)).thenReturn(0L);
    when(tenantRepo.countByStatus(Tenant.TenantStatus.DELETED)).thenReturn(0L);
    when(tenantRepo.countByPlanAndStatus()).thenReturn(List.of());
    when(policyService.countActive()).thenReturn(0L);
    when(flagService.countActiveFlags()).thenReturn(0L);

    mvc.perform(get("/v1/metrics/business"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tenants.active").value(0))
        .andExpect(jsonPath("$.tenants.total").value(0))
        .andExpect(jsonPath("$.active_policies").value(0))
        .andExpect(jsonPath("$.active_flags").value(0));
  }
}
