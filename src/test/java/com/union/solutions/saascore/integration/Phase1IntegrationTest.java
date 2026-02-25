package com.union.solutions.saascore.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
@Testcontainers
class Phase1IntegrationTest {

  @Container
  @SuppressWarnings("resource") // lifecycle managed by Testcontainers JUnit extension
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
  }

  @Test
  void readyz_returnsAggregatedHealth() throws Exception {
    MvcResult result = mvc.perform(get("/readyz")).andExpect(status().isOk()).andReturn();
    JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
    assertThat(body.get("db").asText()).isEqualTo("UP");
    assertThat(body.get("status").asText()).isEqualTo("UP");
    assertThat(body.has("redis")).isTrue();
    assertThat(body.has("rabbitmq")).isTrue();
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
                    .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
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
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
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
                    .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
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

    mvc.perform(get("/v1/policies/" + policyId).header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNotFound());
  }

  @Test
  void policySoftDelete_hidesFromList() throws Exception {
    MvcResult createResult =
        mvc.perform(
                post("/v1/policies")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                    .content(
                        "{\"permissionCode\":\"soft:delete:test\",\"effect\":\"ALLOW\",\"allowedPlans\":[],\"allowedRegions\":[],\"enabled\":true,\"notes\":\"will be soft-deleted\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
    String policyId = created.get("id").asText();

    mvc.perform(delete("/v1/policies/" + policyId).header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNoContent());

    mvc.perform(get("/v1/policies/" + policyId).header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNotFound());
  }

  @Test
  void flagCrud_worksEndToEnd() throws Exception {
    String tenantId = "00000000-0000-0000-0000-000000000001";

    MvcResult createResult =
        mvc.perform(
                post("/v1/tenants/" + tenantId + "/flags")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                    .content(
                        "{\"name\":\"test_flag\",\"enabled\":true,\"rolloutPercent\":50,\"allowedRoles\":[\"admin\"]}"))
            .andExpect(status().isCreated())
            .andReturn();
    JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
    assertThat(created.get("name").asText()).isEqualTo("test_flag");

    mvc.perform(
            get("/v1/tenants/" + tenantId + "/flags")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk());

    mvc.perform(
            patch("/v1/tenants/" + tenantId + "/flags/test_flag")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content("{\"enabled\":false}"))
        .andExpect(status().isOk());

    mvc.perform(
            delete("/v1/tenants/" + tenantId + "/flags/test_flag")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNoContent());
  }

  @Test
  void flagSoftDelete_hidesFromList() throws Exception {
    String tenantId = "00000000-0000-0000-0000-000000000001";

    mvc.perform(
            post("/v1/tenants/" + tenantId + "/flags")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(
                    "{\"name\":\"soft_del_flag\",\"enabled\":true,\"rolloutPercent\":100,\"allowedRoles\":[]}"))
        .andExpect(status().isCreated());

    mvc.perform(
            delete("/v1/tenants/" + tenantId + "/flags/soft_del_flag")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNoContent());

    MvcResult listResult =
        mvc.perform(
                get("/v1/tenants/" + tenantId + "/flags")
                    .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andReturn();
    JsonNode flags = objectMapper.readTree(listResult.getResponse().getContentAsString());
    boolean found = false;
    for (JsonNode flag : flags) {
      if ("soft_del_flag".equals(flag.get("name").asText())) found = true;
    }
    assertThat(found).isFalse();
  }

  @Test
  void abacDeny_isAudited() throws Exception {
    mvc.perform(
            post("/v1/policies")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(
                    "{\"permissionCode\":\"tenants:write\",\"effect\":\"DENY\",\"allowedPlans\":[\"free\"],\"allowedRegions\":[],\"enabled\":true,\"notes\":\"deny free\"}"))
        .andExpect(status().isCreated());

    mvc.perform(
            post("/v1/tenants")
                .header("Authorization", "Bearer " + freeToken)
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
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
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
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

  @Test
  void businessMetrics_returnsData() throws Exception {
    MvcResult result =
        mvc.perform(get("/v1/metrics/business").header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andReturn();
    JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
    assertThat(body.has("tenants")).isTrue();
    assertThat(body.get("tenants").has("active")).isTrue();
    assertThat(body.get("tenants").has("total")).isTrue();
    assertThat(body.has("active_policies")).isTrue();
    assertThat(body.has("active_flags")).isTrue();
    assertThat(body.has("tenants_by_plan")).isTrue();
  }

  @Test
  void tenantSnapshot_returnsData() throws Exception {
    String tenantId = "00000000-0000-0000-0000-000000000002";
    mvc.perform(
            get("/v1/tenants/" + tenantId + "/snapshot")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.plan").value("pro"));

    mvc.perform(
            get("/v1/tenants/" + tenantId + "/policies")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk());

    mvc.perform(
            get("/v1/tenants/" + tenantId + "/flags")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk());
  }

  @Test
  void cursorPagination_tenantsEndpoint() throws Exception {
    for (int i = 0; i < 3; i++) {
      mvc.perform(
          post("/v1/tenants")
              .header("Authorization", "Bearer " + adminToken)
              .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
              .content(
                  "{\"name\":\"Cursor Corp "
                      + i
                      + "\",\"plan\":\"pro\",\"region\":\"us-east-1\"}"));
    }

    MvcResult result =
        mvc.perform(
                get("/v1/tenants?cursor="
                        + java.util.Base64.getUrlEncoder()
                            .withoutPadding()
                            .encodeToString("1970-01-01T00:00:00Z".getBytes())
                        + "&limit=2")
                    .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andReturn();
    JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
    assertThat(body.has("items")).isTrue();
    assertThat(body.has("hasMore")).isTrue();
    assertThat(body.get("items").size()).isLessThanOrEqualTo(2);
  }

  @Test
  void cursorPagination_auditEndpoint() throws Exception {
    MvcResult result =
        mvc.perform(
                get("/v1/audit?cursor="
                        + java.util.Base64.getUrlEncoder()
                            .withoutPadding()
                            .encodeToString("2099-01-01T00:00:00Z".getBytes())
                        + "&limit=5")
                    .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andReturn();
    JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
    assertThat(body.has("items")).isTrue();
    assertThat(body.has("hasMore")).isTrue();
  }

  @Test
  void policyUpdate_worksCorrectly() throws Exception {
    MvcResult createResult =
        mvc.perform(
                post("/v1/policies")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                    .content(
                        "{\"permissionCode\":\"update:test\",\"effect\":\"ALLOW\",\"allowedPlans\":[],\"allowedRegions\":[],\"enabled\":true,\"notes\":\"original\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
    String policyId = created.get("id").asText();

    MvcResult updateResult =
        mvc.perform(
                patch("/v1/policies/" + policyId)
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                    .content("{\"notes\":\"updated\",\"enabled\":false}"))
            .andExpect(status().isOk())
            .andReturn();
    JsonNode updated = objectMapper.readTree(updateResult.getResponse().getContentAsString());
    assertThat(updated.get("notes").asText()).isEqualTo("updated");
    assertThat(updated.get("enabled").asBoolean()).isFalse();
  }

  @Test
  void tenantSearch_filtersWork() throws Exception {
    MvcResult result =
        mvc.perform(
                get("/v1/tenants?plan=enterprise").header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andReturn();
    JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
    for (JsonNode tenant : body.get("content")) {
      assertThat(tenant.get("plan").asText()).isEqualTo("enterprise");
    }
  }

  @Test
  void auditLog_filtersWork() throws Exception {
    mvc.perform(
            get("/v1/audit?actorSub=admin@test").header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray());
  }

  @Test
  void rateLimitHeaders_arePresent() throws Exception {
    MvcResult result =
        mvc.perform(get("/v1/me").header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andReturn();
    assertThat(result.getResponse().getHeader("X-RateLimit-Limit")).isNotNull();
    assertThat(result.getResponse().getHeader("X-RateLimit-Remaining")).isNotNull();
  }

  @Test
  void flagDuplicate_returns400() throws Exception {
    String tenantId = "00000000-0000-0000-0000-000000000001";
    String flagBody =
        "{\"name\":\"dup_flag\",\"enabled\":true,\"rolloutPercent\":100,\"allowedRoles\":[]}";

    mvc.perform(
            post("/v1/tenants/" + tenantId + "/flags")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(flagBody))
        .andExpect(status().isCreated());

    mvc.perform(
            post("/v1/tenants/" + tenantId + "/flags")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(flagBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  void nonExistentTenant_returns404() throws Exception {
    mvc.perform(
            get("/v1/tenants/99999999-9999-9999-9999-999999999999")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNotFound());
  }

  @Test
  void nonExistentPolicy_returns404() throws Exception {
    mvc.perform(
            get("/v1/policies/99999999-9999-9999-9999-999999999999")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNotFound());
  }
}
