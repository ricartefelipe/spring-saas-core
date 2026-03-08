package com.union.solutions.saascore.unit.adapters.in.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.union.solutions.saascore.adapters.in.rest.TenantController;
import com.union.solutions.saascore.application.abac.AbacContext;
import com.union.solutions.saascore.application.abac.AbacEvaluator;
import com.union.solutions.saascore.application.abac.AbacResult;
import com.union.solutions.saascore.application.tenant.TenantUseCase;
import com.union.solutions.saascore.domain.Tenant;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class TenantControllerTest {

  @Mock TenantUseCase tenantUseCase;
  @Mock AbacEvaluator abacEvaluator;

  @BeforeEach
  void setUp() {
    org.mockito.Mockito.lenient()
        .when(abacEvaluator.evaluate(org.mockito.ArgumentMatchers.any(AbacContext.class)))
        .thenReturn(AbacResult.allow());
  }

  @Test
  void getById_found_returns200AndBody() throws Exception {
    UUID id = UUID.randomUUID();
    Tenant t =
        new Tenant(
            id,
            "Acme",
            "pro",
            "us-east-1",
            Tenant.TenantStatus.ACTIVE,
            Instant.now(),
            Instant.now());
    org.mockito.Mockito.when(tenantUseCase.getById(id)).thenReturn(Optional.of(t));

    MockMvc mvc =
        MockMvcBuilders.standaloneSetup(new TenantController(tenantUseCase, abacEvaluator)).build();

    mvc.perform(get("/v1/tenants/{id}", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Acme"))
        .andExpect(jsonPath("$.plan").value("pro"))
        .andExpect(jsonPath("$.region").value("us-east-1"));
  }

  @Test
  void getById_notFound_returns404() throws Exception {
    UUID id = UUID.randomUUID();
    org.mockito.Mockito.when(tenantUseCase.getById(id)).thenReturn(Optional.empty());

    MockMvc mvc =
        MockMvcBuilders.standaloneSetup(new TenantController(tenantUseCase, abacEvaluator)).build();

    mvc.perform(get("/v1/tenants/{id}", id)).andExpect(status().isNotFound());
  }

  @Test
  void list_invalidStatus_returns400() throws Exception {
    MockMvc mvc =
        MockMvcBuilders.standaloneSetup(new TenantController(tenantUseCase, abacEvaluator)).build();

    mvc.perform(get("/v1/tenants").param("status", "INVALID")).andExpect(status().isBadRequest());
  }

  @Test
  void create_abacDeny_returns403() throws Exception {
    org.mockito.Mockito.when(
            abacEvaluator.evaluate(org.mockito.ArgumentMatchers.any(AbacContext.class)))
        .thenReturn(AbacResult.deny(null, "denied_by_policy"));

    MockMvc mvc =
        MockMvcBuilders.standaloneSetup(new TenantController(tenantUseCase, abacEvaluator)).build();

    mvc.perform(
            post("/v1/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Acme\",\"plan\":\"pro\",\"region\":\"us-east-1\"}"))
        .andExpect(status().isForbidden());
  }
}
