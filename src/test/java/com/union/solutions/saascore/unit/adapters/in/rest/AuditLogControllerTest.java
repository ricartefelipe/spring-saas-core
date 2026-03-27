package com.union.solutions.saascore.unit.adapters.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.union.solutions.saascore.adapters.in.rest.AuditLogController;
import com.union.solutions.saascore.adapters.out.persistence.AuditLogEntity;
import com.union.solutions.saascore.adapters.out.persistence.AuditLogJpaRepository;
import com.union.solutions.saascore.application.abac.AbacContext;
import com.union.solutions.saascore.application.abac.AbacEvaluator;
import com.union.solutions.saascore.application.abac.AbacResult;
import com.union.solutions.saascore.config.ProblemDetailsConfig;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuditLogControllerTest {

  @Mock AuditLogJpaRepository auditRepo;
  @Mock AbacEvaluator abacEvaluator;

  private MockMvc mvc;
  private AuditLogController controller;

  @BeforeEach
  void setUp() {
    ObjectMapper om = new ObjectMapper();
    om.registerModule(new JavaTimeModule());
    controller = new AuditLogController(auditRepo, abacEvaluator, om);
    mvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new ProblemDetailsConfig())
            .build();
  }

  @Test
  void list_withAuditReadPermission_returns200() throws Exception {
    when(abacEvaluator.evaluate(any(AbacContext.class))).thenReturn(AbacResult.allow());

    AuditLogEntity entity = makeAuditEntity("TENANT_CREATED");
    when(auditRepo.search(
            isNull(),
            eq(""),
            eq(""),
            eq(""),
            any(Instant.class),
            any(Instant.class),
            any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(entity)));

    mvc.perform(get("/v1/audit"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[0].action").value("TENANT_CREATED"));
  }

  @Test
  void list_withoutAuditReadPermission_returns403() throws Exception {
    doThrow(new AccessDeniedException("ABAC denied"))
        .when(abacEvaluator)
        .enforceOrThrow(anyString());

    mvc.perform(get("/v1/audit")).andExpect(status().isForbidden());
  }

  @Test
  void export_jsonFormat_streamsItemsAndCount() throws Exception {
    when(abacEvaluator.evaluate(any(AbacContext.class))).thenReturn(AbacResult.allow());

    AuditLogEntity entity = makeAuditEntity("POLICY_CREATED");
    when(auditRepo.search(
            isNull(),
            eq(""),
            eq(""),
            eq(""),
            any(Instant.class),
            any(Instant.class),
            any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(entity)));

    Instant from = Instant.parse("2025-01-01T00:00:00Z");
    Instant to = Instant.parse("2026-12-31T23:59:59Z");

    ResponseEntity<?> response = controller.export(null, null, from, to, "json", 10000);
    assertThat(response.getStatusCode().value()).isEqualTo(200);

    StreamingResponseBody body = (StreamingResponseBody) response.getBody();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    body.writeTo(out);
    String json = out.toString();

    assertThat(json).contains("\"count\":1");
    assertThat(json).contains("\"items\":[");
    assertThat(json).contains("POLICY_CREATED");
  }

  @Test
  void export_csvFormat_streamsHeaderAndRows() throws Exception {
    when(abacEvaluator.evaluate(any(AbacContext.class))).thenReturn(AbacResult.allow());

    AuditLogEntity entity = makeAuditEntity("FLAG_CREATED");
    when(auditRepo.search(
            isNull(),
            eq(""),
            eq(""),
            eq(""),
            any(Instant.class),
            any(Instant.class),
            any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(entity)));

    Instant from = Instant.parse("2025-01-01T00:00:00Z");
    Instant to = Instant.parse("2026-12-31T23:59:59Z");

    ResponseEntity<?> response = controller.export(null, null, from, to, "csv", 10000);
    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getHeaders().getFirst("Content-Disposition")).contains("audit-export.csv");

    String csv = (String) response.getBody();

    assertThat(csv).startsWith("id,tenantId,");
    assertThat(csv).contains("FLAG_CREATED");
  }

  @Test
  void export_withoutPermission_returns403() throws Exception {
    doThrow(new AccessDeniedException("ABAC denied"))
        .when(abacEvaluator)
        .enforceOrThrow(anyString());

    mvc.perform(
            get("/v1/audit/export")
                .param("from", "2025-01-01T00:00:00Z")
                .param("to", "2026-12-31T23:59:59Z"))
        .andExpect(status().isForbidden());
  }

  private static AuditLogEntity makeAuditEntity(String action) {
    AuditLogEntity e = new AuditLogEntity();
    e.setTenantId(UUID.randomUUID());
    e.setActorSub("admin@test");
    e.setActorRoles("[admin]");
    e.setActorPerms("[audit:read]");
    e.setAction(action);
    e.setResourceType("tenant");
    e.setResourceId(UUID.randomUUID().toString());
    e.setMethod("POST");
    e.setPath("/v1/tenants");
    e.setStatusCode(201);
    e.setCorrelationId("corr-1");
    e.setCreatedAt(Instant.now());
    return e;
  }
}
