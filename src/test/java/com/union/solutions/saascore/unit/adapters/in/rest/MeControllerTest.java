package com.union.solutions.saascore.unit.adapters.in.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.union.solutions.saascore.adapters.in.rest.MeController;
import com.union.solutions.saascore.config.TenantContext;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MeControllerTest {

  private final MockMvc mvc =
      MockMvcBuilders.standaloneSetup(new MeController()).build();

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Test
  void me_returnsCurrentUserInfo() throws Exception {
    UUID tenantId = UUID.randomUUID();
    TenantContext.setTenantId(tenantId);
    TenantContext.setPlan("pro");
    TenantContext.setRegion("us-east-1");
    TenantContext.setRoles(List.of("admin", "viewer"));
    TenantContext.setPerms(List.of("tenant:read", "tenant:write"));
    TenantContext.setCorrelationId("corr-abc");

    mvc.perform(
            get("/v1/me")
                .principal(new TestingAuthenticationToken("user@test.com", null)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sub").value("user@test.com"))
        .andExpect(jsonPath("$.tenant_id").value(tenantId.toString()))
        .andExpect(jsonPath("$.plan").value("pro"))
        .andExpect(jsonPath("$.region").value("us-east-1"))
        .andExpect(jsonPath("$.roles[0]").value("admin"))
        .andExpect(jsonPath("$.roles[1]").value("viewer"))
        .andExpect(jsonPath("$.perms[0]").value("tenant:read"))
        .andExpect(jsonPath("$.correlation_id").value("corr-abc"));
  }

  @Test
  void me_withNullAuth_returnsEmptySub() throws Exception {
    TenantContext.setPlan("");
    TenantContext.setRegion("");

    mvc.perform(get("/v1/me"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sub").value(""));
  }

  @Test
  void me_withEmptyContext_returnsDefaults() throws Exception {
    mvc.perform(
            get("/v1/me")
                .principal(new TestingAuthenticationToken("admin", null)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sub").value("admin"))
        .andExpect(jsonPath("$.tenant_id").value(""))
        .andExpect(jsonPath("$.plan").value(""))
        .andExpect(jsonPath("$.region").value(""))
        .andExpect(jsonPath("$.correlation_id").value(""));
  }
}
