package com.union.solutions.saascore.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.union.solutions.saascore.application.port.TokenIssuer;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class Phase1IntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("saascore_test")
          .withUsername("test")
          .withPassword("test");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper objectMapper;
  @Autowired TokenIssuer tokenIssuer;

  private String adminToken;
  private String freeToken;

  @BeforeEach
  void setUp() {
    adminToken =
        tokenIssuer.issue(
            "admin@test",
            "00000000-0000-0000-0000-000000000099",
            List.of("admin"),
            List.of(
                "tenants:read",
                "tenants:write",
                "policies:read",
                "policies:write",
                "flags:read",
                "flags:write"),
            "enterprise",
            "us-east-1");
    freeToken =
        tokenIssuer.issue(
            "free@test",
            "00000000-0000-0000-0000-000000000099",
            List.of("user"),
            List.of("admin:write"),
            "free",
            "us-east-1");
  }

  @Test
  void healthEndpoints_areAccessible() throws Exception {
    mvc.perform(get("/healthz")).andExpect(status().isOk());
    mvc.perform(get("/readyz")).andExpect(status().isOk());
  }

  @Test
  void me_withoutToken_returns401() throws Exception {
    mvc.perform(get("/v1/me")).andExpect(status().isUnauthorized());
  }

  @Test
  void me_withToken_returnsUserInfo() throws Exception {
    MvcResult result =
        mvc.perform(
                get("/v1/me")
                    .header("Authorization", "Bearer " + adminToken)
                    .header("X-Correlation-Id", "test-corr-1"))
            .andExpect(status().isOk())
            .andReturn();
    JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
    assertThat(body.get("sub").asText()).isEqualTo("admin@test");
    assertThat(body.get("correlation_id").asText()).isEqualTo("test-corr-1");
  }

  @Test
  void tenantCrud_worksEndToEnd() throws Exception {
    MvcResult createResult =
        mvc.perform(
                post("/v1/tenants")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Test Corp\",\"plan\":\"pro\",\"region\":\"us-east-1\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
    String tenantId = created.get("id").asText();

    mvc.perform(get("/v1/tenants/" + tenantId).header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk());

    mvc.perform(get("/v1/tenants").header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray());

    mvc.perform(
            patch("/v1/tenants/" + tenantId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Updated Corp\"}"))
        .andExpect(status().isOk());

    mvc.perform(delete("/v1/tenants/" + tenantId).header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNoContent());
  }

  @Test
  void policyCrud_worksEndToEnd() throws Exception {
    MvcResult createResult =
        mvc.perform(
                post("/v1/policies")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"permissionCode\":\"test:action\",\"effect\":\"ALLOW\",\"allowedPlans\":[\"pro\"],\"allowedRegions\":[],\"enabled\":true,\"notes\":\"test\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
    String policyId = created.get("id").asText();

    mvc.perform(get("/v1/policies/" + policyId).header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk());

    mvc.perform(delete("/v1/policies/" + policyId).header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNoContent());
  }

  @Test
  void abacDeny_isAudited() throws Exception {
    mvc.perform(
            post("/v1/policies")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"permissionCode\":\"tenants:write\",\"effect\":\"DENY\",\"allowedPlans\":[\"free\"],\"allowedRegions\":[],\"enabled\":true,\"notes\":\"deny free\"}"))
        .andExpect(status().isCreated());

    mvc.perform(
            post("/v1/tenants")
                .header("Authorization", "Bearer " + freeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Denied Corp\",\"plan\":\"free\",\"region\":\"us-east-1\"}"))
        .andExpect(status().isForbidden());

    MvcResult auditResult =
        mvc.perform(
                get("/v1/audit?action=ACCESS_DENIED")
                    .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andReturn();
    JsonNode audit = objectMapper.readTree(auditResult.getResponse().getContentAsString());
    assertThat(audit.get("totalElements").asInt()).isGreaterThan(0);
  }

  @Test
  void devToken_endpointWorks() throws Exception {
    mvc.perform(
            post("/v1/dev/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"sub\":\"dev@test\",\"tid\":\"00000000-0000-0000-0000-000000000001\",\"roles\":[\"admin\"],\"perms\":[\"test:read\"],\"plan\":\"pro\",\"region\":\"us-east-1\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.access_token").isNotEmpty());
  }

  @Test
  void correlationId_isReturnedInHeader() throws Exception {
    mvc.perform(get("/healthz").header("X-Correlation-Id", "my-corr-123"))
        .andExpect(status().isOk())
        .andExpect(header().string("X-Correlation-Id", "my-corr-123"));
  }
}
