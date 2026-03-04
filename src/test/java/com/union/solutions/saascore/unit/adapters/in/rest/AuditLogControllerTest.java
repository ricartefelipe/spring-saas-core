package com.union.solutions.saascore.unit.adapters.in.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.union.solutions.saascore.adapters.in.rest.AuditLogController;
import com.union.solutions.saascore.adapters.out.persistence.AuditLogEntity;
import com.union.solutions.saascore.adapters.out.persistence.AuditLogJpaRepository;
import com.union.solutions.saascore.application.abac.AbacContext;
import com.union.solutions.saascore.application.abac.AbacEvaluator;
import com.union.solutions.saascore.application.abac.AbacResult;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AuditLogControllerTest {

  @Mock AuditLogJpaRepository auditRepo;
  @Mock AbacEvaluator abacEvaluator;

  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    mvc = MockMvcBuilders.standaloneSetup(new AuditLogController(auditRepo, abacEvaluator)).build();
  }

  @Test
  void list_withAuditReadPermission_returns200() throws Exception {
    when(abacEvaluator.evaluate(any(AbacContext.class))).thenReturn(AbacResult.allow());

    AuditLogEntity entity = makeAuditEntity("TENANT_CREATED");
    when(auditRepo.search(
            isNull(), eq(""), eq(""), eq(""), any(Instant.class), any(Instant.class), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(entity)));

    mvc.perform(get("/v1/audit"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[0].action").value("TENANT_CREATED"));
  }

  @Test
  void list_withoutAuditReadPermission_returns403() throws Exception {
    when(abacEvaluator.evaluate(any(AbacContext.class)))
        .thenReturn(AbacResult.deny(null, "no_matching_allow_policy"));

    mvc.perform(get("/v1/audit")).andExpect(status().isForbidden());
  }

  @Test
  void export_withAuditReadPermission_returns200() throws Exception {
    when(abacEvaluator.evaluate(any(AbacContext.class))).thenReturn(AbacResult.allow());

    AuditLogEntity entity = makeAuditEntity("POLICY_CREATED");
    when(auditRepo.search(
            isNull(), eq(""), eq(""), eq(""), any(Instant.class), any(Instant.class), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(entity)));

    Instant from = Instant.parse("2025-01-01T00:00:00Z");
    Instant to = Instant.parse("2026-12-31T23:59:59Z");

    mvc.perform(
            get("/v1/audit/export")
                .param("from", from.toString())
                .param("to", to.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.count").value(1))
        .andExpect(jsonPath("$.items").isArray());
  }

  @Test
  void export_csvFormat_returnsTextCsv() throws Exception {
    when(abacEvaluator.evaluate(any(AbacContext.class))).thenReturn(AbacResult.allow());

    AuditLogEntity entity = makeAuditEntity("FLAG_CREATED");
    when(auditRepo.search(
            isNull(), eq(""), eq(""), eq(""), any(Instant.class), any(Instant.class), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(entity)));

    mvc.perform(
            get("/v1/audit/export")
                .param("from", "2025-01-01T00:00:00Z")
                .param("to", "2026-12-31T23:59:59Z")
                .param("format", "csv"))
        .andExpect(status().isOk());
  }

  @Test
  void export_withoutPermission_returns403() throws Exception {
    when(abacEvaluator.evaluate(any(AbacContext.class)))
        .thenReturn(AbacResult.deny(null, "no_matching_allow_policy"));

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
