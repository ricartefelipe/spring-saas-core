package com.union.solutions.saascore.unit.adapters.in.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.union.solutions.saascore.adapters.in.rest.PolicyController;
import com.union.solutions.saascore.application.abac.AbacContext;
import com.union.solutions.saascore.application.abac.AbacEvaluator;
import com.union.solutions.saascore.application.abac.AbacResult;
import com.union.solutions.saascore.application.service.PolicyService;
import com.union.solutions.saascore.domain.Policy;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class PolicyControllerTest {

  @Mock PolicyService policyService;
  @Mock AbacEvaluator abacEvaluator;

  private MockMvc mvc;
  private UUID policyId;

  @BeforeEach
  void setUp() {
    mvc =
        MockMvcBuilders.standaloneSetup(new PolicyController(policyService, abacEvaluator))
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .build();
    policyId = UUID.randomUUID();
  }

  @Test
  void create_withPoliciesWritePermission_returns201() throws Exception {
    when(abacEvaluator.evaluate(any(AbacContext.class))).thenReturn(AbacResult.allow());

    Policy created =
        makePolicy(
            policyId,
            "orders:read",
            Policy.Effect.ALLOW,
            List.of("pro"),
            List.of("us-east-1"),
            true,
            "test");
    when(policyService.create(
            eq("orders:read"),
            eq(Policy.Effect.ALLOW),
            eq(List.of("pro")),
            eq(List.of("us-east-1")),
            eq(true),
            eq("test")))
        .thenReturn(created);

    mvc.perform(
            post("/v1/policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"permissionCode":"orders:read","effect":"ALLOW","allowedPlans":["pro"],"allowedRegions":["us-east-1"],"enabled":true,"notes":"test"}
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.permissionCode").value("orders:read"))
        .andExpect(jsonPath("$.effect").value("ALLOW"))
        .andExpect(jsonPath("$.enabled").value(true));
  }

  @Test
  void create_withoutPoliciesWritePermission_returns403() throws Exception {
    when(abacEvaluator.evaluate(any(AbacContext.class)))
        .thenReturn(AbacResult.deny(null, "no_matching_allow_policy"));

    mvc.perform(
            post("/v1/policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"permissionCode":"orders:read","effect":"ALLOW","allowedPlans":[],"allowedRegions":[],"enabled":true,"notes":null}
                    """))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403));
  }

  @Test
  void list_withPoliciesReadPermission_returns200() throws Exception {
    when(abacEvaluator.evaluate(any(AbacContext.class))).thenReturn(AbacResult.allow());

    Policy policy =
        makePolicy(
            policyId,
            "orders:read",
            Policy.Effect.ALLOW,
            List.of("pro"),
            List.of("us-east-1"),
            true,
            null);
    when(policyService.search(isNull(), isNull(), isNull(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(policy)));

    mvc.perform(get("/v1/policies"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[0].permissionCode").value("orders:read"))
        .andExpect(jsonPath("$.content[0].effect").value("ALLOW"));
  }

  @Test
  void getById_found_returns200() throws Exception {
    when(abacEvaluator.evaluate(any(AbacContext.class))).thenReturn(AbacResult.allow());

    Policy policy =
        makePolicy(
            policyId,
            "orders:read",
            Policy.Effect.ALLOW,
            List.of("pro"),
            List.of("us-east-1"),
            true,
            null);
    when(policyService.getById(policyId)).thenReturn(Optional.of(policy));

    mvc.perform(get("/v1/policies/{id}", policyId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.permissionCode").value("orders:read"))
        .andExpect(jsonPath("$.effect").value("ALLOW"));
  }

  @Test
  void getById_notFound_returns404() throws Exception {
    when(abacEvaluator.evaluate(any(AbacContext.class))).thenReturn(AbacResult.allow());
    when(policyService.getById(policyId)).thenReturn(Optional.empty());

    mvc.perform(get("/v1/policies/{id}", policyId)).andExpect(status().isNotFound());
  }

  @Test
  void update_withPoliciesWritePermission_returns200() throws Exception {
    when(abacEvaluator.evaluate(any(AbacContext.class))).thenReturn(AbacResult.allow());

    Policy updated =
        makePolicy(
            policyId,
            "orders:write",
            Policy.Effect.ALLOW,
            List.of("pro"),
            List.of("us-east-1"),
            false,
            "updated");
    when(policyService.update(
            eq(policyId),
            eq("orders:write"),
            eq(Policy.Effect.ALLOW),
            eq(List.of("pro")),
            eq(List.of("us-east-1")),
            eq(false),
            eq("updated")))
        .thenReturn(Optional.of(updated));

    mvc.perform(
            patch("/v1/policies/{id}", policyId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"permissionCode":"orders:write","effect":"ALLOW","allowedPlans":["pro"],"allowedRegions":["us-east-1"],"enabled":false,"notes":"updated"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.permissionCode").value("orders:write"))
        .andExpect(jsonPath("$.enabled").value(false));
  }

  @Test
  void delete_withPoliciesWritePermission_returns204() throws Exception {
    when(abacEvaluator.evaluate(any(AbacContext.class))).thenReturn(AbacResult.allow());
    when(policyService.softDelete(policyId)).thenReturn(true);

    mvc.perform(delete("/v1/policies/{id}", policyId)).andExpect(status().isNoContent());
  }

  @Test
  void delete_notFound_returns404() throws Exception {
    when(abacEvaluator.evaluate(any(AbacContext.class))).thenReturn(AbacResult.allow());
    when(policyService.softDelete(policyId)).thenReturn(false);

    mvc.perform(delete("/v1/policies/{id}", policyId)).andExpect(status().isNotFound());
  }

  private static Policy makePolicy(
      UUID id,
      String permissionCode,
      Policy.Effect effect,
      List<String> allowedPlans,
      List<String> allowedRegions,
      boolean enabled,
      String notes) {
    return new Policy(
        id,
        permissionCode,
        effect,
        allowedPlans,
        allowedRegions,
        enabled,
        notes,
        Instant.now(),
        Instant.now());
  }
}
