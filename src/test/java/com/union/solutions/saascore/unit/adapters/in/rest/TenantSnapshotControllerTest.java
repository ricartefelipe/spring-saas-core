package com.union.solutions.saascore.unit.adapters.in.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.union.solutions.saascore.adapters.in.rest.TenantSnapshotController;
import com.union.solutions.saascore.application.abac.AbacContext;
import com.union.solutions.saascore.application.abac.AbacEvaluator;
import com.union.solutions.saascore.application.abac.AbacResult;
import com.union.solutions.saascore.application.port.UserRepository;
import com.union.solutions.saascore.application.service.FeatureFlagService;
import com.union.solutions.saascore.application.service.PolicyService;
import com.union.solutions.saascore.application.tenant.TenantUseCase;
import com.union.solutions.saascore.config.TenantContext;
import com.union.solutions.saascore.domain.FeatureFlag;
import com.union.solutions.saascore.domain.Policy;
import com.union.solutions.saascore.domain.Tenant;
import com.union.solutions.saascore.domain.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class TenantSnapshotControllerTest {

  @Mock TenantUseCase tenantUseCase;
  @Mock PolicyService policyService;
  @Mock FeatureFlagService flagService;
  @Mock UserRepository userRepo;
  @Mock AbacEvaluator abacEvaluator;

  private MockMvc mvc;
  private UUID tenantId;
  private Tenant tenant;

  @BeforeEach
  void setUp() {
    when(abacEvaluator.evaluate(any(AbacContext.class))).thenReturn(AbacResult.allow());
    mvc =
        MockMvcBuilders.standaloneSetup(
                new TenantSnapshotController(
                    tenantUseCase, policyService, flagService, userRepo, abacEvaluator))
            .build();
    tenantId = UUID.randomUUID();
    tenant =
        new Tenant(
            tenantId,
            "Acme",
            "pro",
            "us-east-1",
            Tenant.TenantStatus.ACTIVE,
            Instant.now(),
            Instant.now());
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Test
  void getSnapshot_returnsTenantData() throws Exception {
    when(tenantUseCase.getById(tenantId)).thenReturn(Optional.of(tenant));

    mvc.perform(get("/v1/tenants/{id}/snapshot", tenantId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(tenantId.toString()))
        .andExpect(jsonPath("$.plan").value("pro"))
        .andExpect(jsonPath("$.region").value("us-east-1"))
        .andExpect(jsonPath("$.status").value("ACTIVE"));
  }

  @Test
  void getSnapshot_withIncludePoliciesAndFlags_returnsAll() throws Exception {
    when(tenantUseCase.getById(tenantId)).thenReturn(Optional.of(tenant));

    Policy policy =
        new Policy(
            UUID.randomUUID(),
            "tenants:read",
            Policy.Effect.ALLOW,
            List.of("pro"),
            List.of(),
            true,
            "note",
            Instant.now(),
            Instant.now());
    when(policyService.getApplicablePolicies("pro", "us-east-1")).thenReturn(List.of(policy));

    FeatureFlag flag =
        new FeatureFlag(
            UUID.randomUUID(),
            tenantId,
            "dark_mode",
            true,
            100,
            List.of("admin"),
            Instant.now(),
            Instant.now());
    when(flagService.listByTenant(tenantId)).thenReturn(List.of(flag));

    mvc.perform(get("/v1/tenants/{id}/snapshot", tenantId).param("include", "policies,flags"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(tenantId.toString()))
        .andExpect(jsonPath("$.policies").isArray())
        .andExpect(jsonPath("$.policies[0].permission_code").value("tenants:read"))
        .andExpect(jsonPath("$.flags").isArray())
        .andExpect(jsonPath("$.flags[0].name").value("dark_mode"));
  }

  @Test
  void getSnapshot_nonExistentTenant_returns404() throws Exception {
    UUID missingId = UUID.randomUUID();
    when(tenantUseCase.getById(missingId)).thenReturn(Optional.empty());

    mvc.perform(get("/v1/tenants/{id}/snapshot", missingId)).andExpect(status().isNotFound());
  }

  @Test
  void getPolicies_returnsPoliciesForTenant() throws Exception {
    when(tenantUseCase.getById(tenantId)).thenReturn(Optional.of(tenant));

    Policy policy =
        new Policy(
            UUID.randomUUID(),
            "flags:write",
            Policy.Effect.ALLOW,
            List.of(),
            List.of(),
            true,
            null,
            Instant.now(),
            Instant.now());
    when(policyService.getApplicablePolicies("pro", "us-east-1")).thenReturn(List.of(policy));

    mvc.perform(get("/v1/tenants/{id}/policies", tenantId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].permission_code").value("flags:write"))
        .andExpect(jsonPath("$[0].effect").value("ALLOW"));
  }

  @Test
  void getPolicies_nonExistentTenant_returns404() throws Exception {
    UUID missingId = UUID.randomUUID();
    when(tenantUseCase.getById(missingId)).thenReturn(Optional.empty());

    mvc.perform(get("/v1/tenants/{id}/policies", missingId)).andExpect(status().isNotFound());
  }

  @Test
  void getFlags_returnsFlagsForTenant() throws Exception {
    when(tenantUseCase.getById(tenantId)).thenReturn(Optional.of(tenant));

    FeatureFlag flag =
        new FeatureFlag(
            UUID.randomUUID(),
            tenantId,
            "beta_ui",
            true,
            50,
            List.of("admin"),
            Instant.now(),
            Instant.now());
    when(flagService.listByTenant(tenantId)).thenReturn(List.of(flag));

    mvc.perform(get("/v1/tenants/{id}/snapshot", tenantId).param("include", "flags"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.flags").isArray())
        .andExpect(jsonPath("$.flags[0].name").value("beta_ui"))
        .andExpect(jsonPath("$.flags[0].enabled").value(true));
  }

  @Test
  void getHealth_returnsLastActivityAndActiveUsersCount() throws Exception {
    when(tenantUseCase.getById(tenantId)).thenReturn(Optional.of(tenant));
    Instant lastLogin = Instant.now().minusSeconds(3600);
    when(userRepo.findMaxLastLoginAtByTenantId(tenantId)).thenReturn(Optional.of(lastLogin));
    when(userRepo.countActiveByTenantId(tenantId)).thenReturn(3L);

    mvc.perform(get("/v1/tenants/{id}/health", tenantId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tenantId").value(tenantId.toString()))
        .andExpect(jsonPath("$.lastActivityAt").value(lastLogin.toString()))
        .andExpect(jsonPath("$.activeUsersCount").value(3));
  }

  @Test
  void getHealth_nonExistentTenant_returns404() throws Exception {
    UUID missingId = UUID.randomUUID();
    when(tenantUseCase.getById(missingId)).thenReturn(Optional.empty());

    mvc.perform(get("/v1/tenants/{id}/health", missingId)).andExpect(status().isNotFound());
  }

  @Test
  void getExport_returnsTenantUsersPoliciesFlags_whenContextMatchesTenant() throws Exception {
    TenantContext.setTenantId(tenantId);
    when(tenantUseCase.getById(tenantId)).thenReturn(Optional.of(tenant));
    User user =
        new User(
            UUID.randomUUID(),
            "admin@acme.com",
            "Admin",
            "hash",
            tenantId,
            List.of("admin"),
            User.UserStatus.ACTIVE,
            Instant.now(),
            Instant.now());
    user.setLastLoginAt(Instant.now().minusSeconds(7200));
    when(userRepo.findByTenantId(tenantId)).thenReturn(List.of(user));
    when(policyService.getApplicablePolicies("pro", "us-east-1")).thenReturn(List.of());
    when(flagService.listByTenant(tenantId)).thenReturn(List.of());

    mvc.perform(get("/v1/tenants/{id}/export", tenantId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tenant.id").value(tenantId.toString()))
        .andExpect(jsonPath("$.tenant.name").value("Acme"))
        .andExpect(jsonPath("$.users").isArray())
        .andExpect(jsonPath("$.users[0].email").value("admin@acme.com"))
        .andExpect(jsonPath("$.users[0].lastLoginAt").exists())
        .andExpect(jsonPath("$.policies").isArray())
        .andExpect(jsonPath("$.featureFlags").isArray());
  }

  @Test
  void getExport_whenContextTenantDifferent_returns403() throws Exception {
    UUID otherTenantId = UUID.randomUUID();
    TenantContext.setTenantId(otherTenantId);

    mvc.perform(get("/v1/tenants/{id}/export", tenantId)).andExpect(status().isForbidden());
  }

  @Test
  void getExport_whenNoTenantContext_returns403() throws Exception {
    mvc.perform(get("/v1/tenants/{id}/export", tenantId)).andExpect(status().isForbidden());
  }
}
