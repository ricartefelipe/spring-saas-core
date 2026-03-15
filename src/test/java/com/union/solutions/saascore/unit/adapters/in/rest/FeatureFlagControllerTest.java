package com.union.solutions.saascore.unit.adapters.in.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.union.solutions.saascore.config.ProblemDetailsConfig;
import com.union.solutions.saascore.adapters.in.rest.FeatureFlagController;
import com.union.solutions.saascore.application.abac.AbacContext;
import com.union.solutions.saascore.application.abac.AbacEvaluator;
import com.union.solutions.saascore.application.abac.AbacResult;
import com.union.solutions.saascore.application.service.FeatureFlagService;
import com.union.solutions.saascore.domain.FeatureFlag;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FeatureFlagControllerTest {

  @Mock FeatureFlagService flagService;
  @Mock AbacEvaluator abacEvaluator;

  private MockMvc mvc;
  private final UUID tenantId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    mvc =
        MockMvcBuilders.standaloneSetup(new FeatureFlagController(flagService, abacEvaluator))
            .setControllerAdvice(new ProblemDetailsConfig())
            .build();
  }

  @Test
  void list_withFlagsReadPermission_returns200() throws Exception {
    when(abacEvaluator.evaluate(any(AbacContext.class))).thenReturn(AbacResult.allow());

    FeatureFlag flag = makeFlag("dark_mode", true, 100);
    when(flagService.listByTenant(tenantId)).thenReturn(List.of(flag));

    mvc.perform(get("/v1/tenants/{tenantId}/flags", tenantId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].name").value("dark_mode"))
        .andExpect(jsonPath("$[0].enabled").value(true));
  }

  @Test
  void list_withoutFlagsReadPermission_returns403() throws Exception {
    doThrow(new AccessDeniedException("ABAC denied"))
        .when(abacEvaluator)
        .enforceOrThrow(anyString());

    mvc.perform(get("/v1/tenants/{tenantId}/flags", tenantId))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403));
  }

  @Test
  void create_withFlagsWritePermission_returns201() throws Exception {
    when(abacEvaluator.evaluate(any(AbacContext.class))).thenReturn(AbacResult.allow());

    FeatureFlag created = makeFlag("new_feature", true, 50);
    when(flagService.create(eq(tenantId), eq("new_feature"), eq(true), eq(50), any()))
        .thenReturn(created);

    mvc.perform(
            post("/v1/tenants/{tenantId}/flags", tenantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"name\":\"new_feature\",\"enabled\":true,\"rolloutPercent\":50,\"allowedRoles\":[\"admin\"]}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("new_feature"))
        .andExpect(jsonPath("$.enabled").value(true))
        .andExpect(jsonPath("$.rolloutPercent").value(50));
  }

  @Test
  void create_withoutFlagsWritePermission_returns403() throws Exception {
    doThrow(new AccessDeniedException("ABAC denied"))
        .when(abacEvaluator)
        .enforceOrThrow(anyString());

    mvc.perform(
            post("/v1/tenants/{tenantId}/flags", tenantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"name\":\"new_feature\",\"enabled\":true,\"rolloutPercent\":50,\"allowedRoles\":[]}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void update_withValidData_returns200() throws Exception {
    when(abacEvaluator.evaluate(any(AbacContext.class))).thenReturn(AbacResult.allow());

    FeatureFlag updated = makeFlag("dark_mode", false, 25);
    when(flagService.update(eq(tenantId), eq("dark_mode"), eq(false), eq(25), any()))
        .thenReturn(Optional.of(updated));

    mvc.perform(
            patch("/v1/tenants/{tenantId}/flags/{flagName}", tenantId, "dark_mode")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\":false,\"rolloutPercent\":25}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("dark_mode"))
        .andExpect(jsonPath("$.enabled").value(false));
  }

  @Test
  void update_nonExistentFlag_returns404() throws Exception {
    when(abacEvaluator.evaluate(any(AbacContext.class))).thenReturn(AbacResult.allow());
    when(flagService.update(eq(tenantId), eq("missing"), any(), any(), any()))
        .thenReturn(Optional.empty());

    mvc.perform(
            patch("/v1/tenants/{tenantId}/flags/{flagName}", tenantId, "missing")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\":true}"))
        .andExpect(status().isNotFound());
  }

  @Test
  void delete_withFlagsWritePermission_returns204() throws Exception {
    when(abacEvaluator.evaluate(any(AbacContext.class))).thenReturn(AbacResult.allow());
    when(flagService.softDelete(tenantId, "dark_mode")).thenReturn(true);

    mvc.perform(delete("/v1/tenants/{tenantId}/flags/{flagName}", tenantId, "dark_mode"))
        .andExpect(status().isNoContent());
  }

  @Test
  void delete_withoutPermission_returns403() throws Exception {
    doThrow(new AccessDeniedException("ABAC denied"))
        .when(abacEvaluator)
        .enforceOrThrow(anyString());

    mvc.perform(delete("/v1/tenants/{tenantId}/flags/{flagName}", tenantId, "dark_mode"))
        .andExpect(status().isForbidden());
  }

  @Test
  void delete_nonExistentFlag_returns404() throws Exception {
    when(abacEvaluator.evaluate(any(AbacContext.class))).thenReturn(AbacResult.allow());
    when(flagService.softDelete(tenantId, "missing")).thenReturn(false);

    mvc.perform(delete("/v1/tenants/{tenantId}/flags/{flagName}", tenantId, "missing"))
        .andExpect(status().isNotFound());
  }

  private FeatureFlag makeFlag(String name, boolean enabled, int rolloutPercent) {
    return new FeatureFlag(
        UUID.randomUUID(),
        tenantId,
        name,
        enabled,
        rolloutPercent,
        List.of("admin"),
        Instant.now(),
        Instant.now());
  }
}
