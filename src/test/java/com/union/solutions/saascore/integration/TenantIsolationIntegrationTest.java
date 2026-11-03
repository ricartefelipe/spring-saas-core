package com.union.solutions.saascore.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.union.solutions.saascore.application.auth.JwtRolePermissions;
import com.union.solutions.saascore.application.port.TokenIssuer;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class TenantIsolationIntegrationTest {

  @Container
  @SuppressWarnings("resource")
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("saascore_isolation_test")
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
  @Autowired JdbcTemplate jdbc;

  private String platform;
  private String tenantAdmin;
  private String own;
  private String other;

  @BeforeEach
  void setUp() throws Exception {
    List<String> admin = List.of("admin");
    platform =
        tokenIssuer.issue(
            "plataforma@test",
            "*",
            admin,
            JwtRolePermissions.forRoles(admin, true),
            "enterprise",
            "region-a");
    own = createTenant("Proprio " + UUID.randomUUID());
    other = createTenant("Outro " + UUID.randomUUID());
    tenantAdmin =
        tokenIssuer.issue(
            "admin@proprio.test",
            own,
            admin,
            // O token traz todas as permissões: a regra precisa valer mesmo com um token mais
            // amplo.
            JwtRolePermissions.forRoles(admin, true),
            "enterprise",
            "region-a");
  }

  private void insertAuditEvent(String tenantId) {
    jdbc.update(
        "INSERT INTO audit_log (id, tenant_id, action, created_at) VALUES (?, ?, ?, now())",
        UUID.randomUUID(),
        UUID.fromString(tenantId),
        "TEST_EVENT");
  }

  private String createTenant(String name) throws Exception {
    String body =
        mvc.perform(
                json(post("/v1/tenants"), platform)
                    .content(
                        objectMapper.writeValueAsString(
                            java.util.Map.of("name", name, "plan", "pro", "region", "region-a"))))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readTree(body).get("id").asText();
  }

  private static MockHttpServletRequestBuilder json(
      MockHttpServletRequestBuilder request, String token) {
    return request
        .header("Authorization", "Bearer " + token)
        .contentType(MediaType.APPLICATION_JSON);
  }

  private JsonNode getJson(String path, String token) throws Exception {
    String body =
        mvc.perform(json(get(path), token))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readTree(body);
  }

  @Test
  void adminDoLocatarioListaSoOProprio() throws Exception {
    JsonNode page = getJson("/v1/tenants?limit=100", tenantAdmin);
    assertThat(page.get("items")).hasSize(1);
    assertThat(page.get("items").get(0).get("id").asText()).isEqualTo(own);
    assertThat(page.get("hasMore").asBoolean()).isFalse();
  }

  @Test
  void plataformaListaTodos() throws Exception {
    List<String> ids =
        getJson("/v1/tenants?limit=100", platform).get("items").findValuesAsText("id");
    assertThat(ids).contains(own, other);
  }

  @Test
  void adminDoLocatarioLeOProprioENaoLeOutro() throws Exception {
    mvc.perform(json(get("/v1/tenants/" + own), tenantAdmin)).andExpect(status().isOk());
    mvc.perform(json(get("/v1/tenants/" + other), tenantAdmin)).andExpect(status().isForbidden());
    for (String sub : List.of("snapshot", "health", "policies", "export", "flags")) {
      mvc.perform(json(get("/v1/tenants/" + other + "/" + sub), tenantAdmin))
          .andExpect(status().isForbidden());
    }
    mvc.perform(json(get("/v1/tenants/" + own + "/health"), tenantAdmin))
        .andExpect(status().isOk());
  }

  @Test
  void soAPlataformaCriaAlteraEApagaLocatario() throws Exception {
    String patchBody = "{\"plan\":\"enterprise\",\"status\":\"ACTIVE\"}";
    mvc.perform(json(patch("/v1/tenants/" + own), tenantAdmin).content(patchBody))
        .andExpect(status().isForbidden());
    mvc.perform(json(patch("/v1/tenants/" + other), tenantAdmin).content(patchBody))
        .andExpect(status().isForbidden());
    mvc.perform(json(delete("/v1/tenants/" + other), tenantAdmin))
        .andExpect(status().isForbidden());
    mvc.perform(
            json(post("/v1/tenants"), tenantAdmin)
                .content("{\"name\":\"Novo\",\"plan\":\"pro\",\"region\":\"region-a\"}"))
        .andExpect(status().isForbidden());
    mvc.perform(json(patch("/v1/tenants/" + other), platform).content(patchBody))
        .andExpect(status().isOk());
  }

  @Test
  void soAPlataformaEditaPoliticasGlobais() throws Exception {
    mvc.perform(json(get("/v1/policies"), tenantAdmin)).andExpect(status().isOk());
    mvc.perform(
            json(post("/v1/policies"), tenantAdmin)
                .content("{\"permissionCode\":\"x:y\",\"effect\":\"ALLOW\"}"))
        .andExpect(status().isForbidden());
    mvc.perform(json(delete("/v1/policies/" + UUID.randomUUID()), tenantAdmin))
        .andExpect(status().isForbidden());
    mvc.perform(json(patch("/v1/policies/" + UUID.randomUUID()), tenantAdmin).content("{}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void auditoriaFicaPresaAoLocatarioDoToken() throws Exception {
    // A criação pela plataforma e os acessos negados geram eventos em outros locatários.
    mvc.perform(json(get("/v1/tenants/" + other), tenantAdmin)).andExpect(status().isForbidden());
    mvc.perform(json(get("/v1/audit?tenantId=" + other), tenantAdmin))
        .andExpect(status().isForbidden());
    mvc.perform(
            json(
                get(
                    "/v1/audit/export?from=2020-01-01T00:00:00Z&to=2099-01-01T00:00:00Z&tenantId="
                        + other),
                tenantAdmin))
        .andExpect(status().isForbidden());
    // Um evento em cada locatário: sem eles, o laço abaixo não provaria nada.
    insertAuditEvent(own);
    insertAuditEvent(other);
    JsonNode events = getJson("/v1/audit?limit=100", tenantAdmin).get("content");
    assertThat(events).isNotEmpty();
    for (JsonNode event : events) {
      assertThat(event.get("tenantId").asText()).isEqualTo(own);
    }
    // A plataforma continua vendo o evento do outro locatário: o filtro não esvazia a consulta.
    assertThat(getJson("/v1/audit?limit=100&tenantId=" + other, platform).get("content"))
        .isNotEmpty();
  }

  @Test
  void exportacaoDeAuditoriaSemPeriodoRespondeBadRequest() throws Exception {
    mvc.perform(json(get("/v1/audit/export"), tenantAdmin)).andExpect(status().isBadRequest());
  }

  @Test
  void dadosGlobaisSaoSoDaPlataforma() throws Exception {
    for (String path :
        List.of(
            "/v1/metrics/business",
            "/v1/analytics/summary",
            "/v1/analytics/anomalies",
            "/v1/ai/insights",
            "/v1/ai/docs",
            "/v1/ai/docs/tenant/" + own,
            "/v1/ai/recommendations")) {
      mvc.perform(json(get(path), tenantAdmin)).andExpect(status().isForbidden());
    }
    mvc.perform(json(post("/v1/ai/analyze-audit?tenantId=" + other), tenantAdmin))
        .andExpect(status().isForbidden());
    mvc.perform(json(post("/v1/ai/chat"), tenantAdmin).content("{\"message\":\"oi\"}"))
        .andExpect(status().isForbidden());
    mvc.perform(json(get("/v1/metrics/business"), platform)).andExpect(status().isOk());
  }

  @Test
  void cabecalhoDeOutroLocatarioERecusado() throws Exception {
    mvc.perform(json(get("/v1/users"), tenantAdmin).header("X-Tenant-Id", other))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminDoLocatarioNaoRecebePermissoesDePlataformaNoToken() {
    assertThat(JwtRolePermissions.forRoles(List.of("admin"), false))
        .doesNotContain("tenants:write", "policies:write")
        .contains("tenants:read", "policies:read", "users:write");
    assertThat(JwtRolePermissions.forRoles(List.of("admin"), true))
        .contains("tenants:write", "policies:write");
  }
}
