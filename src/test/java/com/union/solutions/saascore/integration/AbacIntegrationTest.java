package com.union.solutions.saascore.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.union.solutions.saascore.application.port.TokenIssuer;
import java.util.List;
import java.util.Objects;
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
@Testcontainers(disabledWithoutDocker = true)
class AbacIntegrationTest {

  @Container
  @SuppressWarnings("resource")
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("saascore_abac_test")
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
  private String freeUserToken;
  private String enterpriseUserToken;
  private String noPermsToken;

  @BeforeEach
  void setUp() {
    adminToken =
        tokenIssuer.issue(
            "admin@test",
            "00000000-0000-0000-0000-000000000099",
            List.of("admin"),
            List.of(
                "tenants:read", "tenants:write",
                "policies:read", "policies:write",
                "flags:read", "flags:write",
                "audit:read"),
            "enterprise",
            "us-east-1");

    freeUserToken =
        tokenIssuer.issue(
            "free-user@test",
            "00000000-0000-0000-0000-000000000099",
            List.of("user"),
            List.of("tenants:write"),
            "free",
            "us-east-1");

    enterpriseUserToken =
        tokenIssuer.issue(
            "enterprise-user@test",
            "00000000-0000-0000-0000-000000000099",
            List.of("user"),
            List.of(
                "tenants:read", "tenants:write",
                "policies:read", "flags:read"),
            "enterprise",
            "us-east-1");

    noPermsToken =
        tokenIssuer.issue(
            "noperms@test",
            "00000000-0000-0000-0000-000000000099",
            List.of("viewer"),
            List.of(),
            "pro",
            "us-east-1");
  }

  @Test
  void userWithCorrectPermissions_canAccessResource() throws Exception {
    mvc.perform(
            get("/v1/tenants")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk());
  }

  @Test
  void userWithoutRequiredPermission_gets403() throws Exception {
    mvc.perform(
            get("/v1/tenants")
                .header("Authorization", "Bearer " + noPermsToken))
        .andExpect(status().isForbidden());
  }

  @Test
  void denyPolicy_blocksFreeUser_denyTakesPrecedence() throws Exception {
    MvcResult createResult =
        mvc.perform(
                post("/v1/policies")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                    .content(
                        "{\"permissionCode\":\"tenants:write\",\"effect\":\"DENY\","
                            + "\"allowedPlans\":[\"free\"],\"allowedRegions\":[],"
                            + "\"enabled\":true,\"notes\":\"deny free plan write\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    String policyId =
        objectMapper
            .readTree(createResult.getResponse().getContentAsString())
            .get("id")
            .asText();

    mvc.perform(
            post("/v1/tenants")
                .header("Authorization", "Bearer " + freeUserToken)
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content("{\"name\":\"Blocked Corp\",\"plan\":\"free\",\"region\":\"us-east-1\"}"))
        .andExpect(status().isForbidden());

    MvcResult auditResult =
        mvc.perform(
                get("/v1/audit?action=ACCESS_DENIED")
                    .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andReturn();
    JsonNode audit = objectMapper.readTree(auditResult.getResponse().getContentAsString());
    assertThat(audit.get("totalElements").asInt()).isGreaterThan(0);

    mvc.perform(
            delete("/v1/policies/" + policyId)
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNoContent());
  }

  @Test
  void enterpriseUser_canAccessEnterprisePolicies() throws Exception {
    MvcResult createResult =
        mvc.perform(
                post("/v1/policies")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                    .content(
                        "{\"permissionCode\":\"tenants:read\",\"effect\":\"ALLOW\","
                            + "\"allowedPlans\":[\"enterprise\"],\"allowedRegions\":[],"
                            + "\"enabled\":true,\"notes\":\"enterprise-only read\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    String policyId =
        objectMapper
            .readTree(createResult.getResponse().getContentAsString())
            .get("id")
            .asText();

    mvc.perform(
            get("/v1/tenants")
                .header("Authorization", "Bearer " + enterpriseUserToken))
        .andExpect(status().isOk());

    mvc.perform(
            delete("/v1/policies/" + policyId)
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNoContent());
  }

  @Test
  void featureFlagAccess_withoutPermission_gets403() throws Exception {
    String tenantId = "00000000-0000-0000-0000-000000000001";

    mvc.perform(
            get("/v1/tenants/" + tenantId + "/flags")
                .header("Authorization", "Bearer " + noPermsToken))
        .andExpect(status().isForbidden());
  }

  @Test
  void featureFlagAccess_withPermission_succeeds() throws Exception {
    String tenantId = "00000000-0000-0000-0000-000000000001";

    mvc.perform(
            get("/v1/tenants/" + tenantId + "/flags")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk());
  }

  @Test
  void auditAccess_withPermission_succeeds() throws Exception {
    mvc.perform(
            get("/v1/audit")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk());
  }

  @Test
  void auditAccess_withoutPermission_gets403() throws Exception {
    mvc.perform(
            get("/v1/audit")
                .header("Authorization", "Bearer " + noPermsToken))
        .andExpect(status().isForbidden());
  }

  @Test
  void withoutToken_returns401() throws Exception {
    mvc.perform(get("/v1/tenants"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void denyPolicy_doesNotAffectDifferentPlan() throws Exception {
    MvcResult createResult =
        mvc.perform(
                post("/v1/policies")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                    .content(
                        "{\"permissionCode\":\"tenants:read\",\"effect\":\"DENY\","
                            + "\"allowedPlans\":[\"free\"],\"allowedRegions\":[],"
                            + "\"enabled\":true,\"notes\":\"deny free read only\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    String policyId =
        objectMapper
            .readTree(createResult.getResponse().getContentAsString())
            .get("id")
            .asText();

    mvc.perform(
            get("/v1/tenants")
                .header("Authorization", "Bearer " + enterpriseUserToken))
        .andExpect(status().isOk());

    mvc.perform(
            delete("/v1/policies/" + policyId)
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNoContent());
  }
}
