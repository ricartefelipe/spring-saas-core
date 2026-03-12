package com.union.solutions.saascore.application.service;

import com.union.solutions.saascore.adapters.out.persistence.AuditLogJpaRepository;
import com.union.solutions.saascore.adapters.out.persistence.FeatureFlagJpaRepository;
import com.union.solutions.saascore.adapters.out.persistence.PolicyJpaRepository;
import com.union.solutions.saascore.application.tenant.TenantUseCase;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generates live API documentation summaries from current tenant, policy, flag and audit data.
 * Designed for LLM consumption.
 */
@Service
public class AiDocsService {

  private static final List<Map<String, String>> API_SURFACE =
      List.of(
          Map.of("method", "GET", "path", "/v1/tenants", "description", "List tenants (paginated)"),
          Map.of("method", "POST", "path", "/v1/tenants", "description", "Create tenant"),
          Map.of("method", "GET", "path", "/v1/tenants/{id}", "description", "Get tenant by ID"),
          Map.of("method", "PATCH", "path", "/v1/tenants/{id}", "description", "Update tenant"),
          Map.of("method", "DELETE", "path", "/v1/tenants/{id}", "description", "Soft delete tenant"),
          Map.of("method", "GET", "path", "/v1/tenants/{id}/snapshot", "description", "Tenant snapshot (plan, region, status; optional policies, flags)"),
          Map.of("method", "GET", "path", "/v1/tenants/{id}/policies", "description", "Policies applicable to tenant"),
          Map.of("method", "POST", "path", "/v1/policies", "description", "Create policy"),
          Map.of("method", "GET", "path", "/v1/policies", "description", "List policies (paginated)"),
          Map.of("method", "GET", "path", "/v1/policies/{id}", "description", "Get policy by ID"),
          Map.of("method", "PATCH", "path", "/v1/policies/{id}", "description", "Update policy"),
          Map.of("method", "DELETE", "path", "/v1/policies/{id}", "description", "Remove policy"),
          Map.of("method", "POST", "path", "/v1/tenants/{tenantId}/flags", "description", "Create feature flag"),
          Map.of("method", "GET", "path", "/v1/tenants/{tenantId}/flags", "description", "List flags for tenant"),
          Map.of("method", "PATCH", "path", "/v1/tenants/{tenantId}/flags/{name}", "description", "Update flag"),
          Map.of("method", "DELETE", "path", "/v1/tenants/{tenantId}/flags/{name}", "description", "Remove flag"),
          Map.of("method", "GET", "path", "/v1/audit", "description", "Query audit log (paginated)"),
          Map.of("method", "GET", "path", "/v1/audit/export", "description", "Export audit log (JSON/CSV)"),
          Map.of("method", "GET", "path", "/v1/ai/docs", "description", "Live API documentation summary"),
          Map.of("method", "GET", "path", "/v1/ai/docs/tenant/{id}", "description", "Tenant-specific documentation"),
          Map.of("method", "GET", "path", "/v1/ai/status", "description", "AI engine status"),
          Map.of("method", "POST", "path", "/v1/ai/analyze-audit", "description", "AI-powered audit analysis"),
          Map.of("method", "POST", "path", "/v1/ai/recommendations", "description", "Governance recommendations"),
          Map.of("method", "POST", "path", "/v1/ai/chat", "description", "AI governance assistant"),
          Map.of("method", "GET", "path", "/v1/ai/insights", "description", "System insights and health indicators"),
          Map.of("method", "GET", "path", "/v1/me", "description", "Current user claims"),
          Map.of("method", "GET", "path", "/v1/users", "description", "List users (tenant-scoped)"),
          Map.of("method", "POST", "path", "/v1/users/invite", "description", "Invite user to tenant"),
          Map.of("method", "GET", "path", "/v1/webhooks", "description", "List webhook endpoints"),
          Map.of("method", "POST", "path", "/v1/webhooks", "description", "Register webhook endpoint"));

  private final AnalyticsService analyticsService;
  private final PolicyJpaRepository policyRepo;
  private final FeatureFlagJpaRepository flagRepo;
  private final AuditLogJpaRepository auditRepo;
  private final TenantUseCase tenantUseCase;
  private final PolicyService policyService;
  private final FeatureFlagService flagService;

  @Value("${app.version:1.0.0-SNAPSHOT}")
  private String appVersion;

  public AiDocsService(
      AnalyticsService analyticsService,
      PolicyJpaRepository policyRepo,
      FeatureFlagJpaRepository flagRepo,
      AuditLogJpaRepository auditRepo,
      TenantUseCase tenantUseCase,
      PolicyService policyService,
      FeatureFlagService flagService) {
    this.analyticsService = analyticsService;
    this.policyRepo = policyRepo;
    this.flagRepo = flagRepo;
    this.auditRepo = auditRepo;
    this.tenantUseCase = tenantUseCase;
    this.policyService = policyService;
    this.flagService = flagService;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> buildLiveDocs() {
    Instant now = Instant.now();
    Instant since24h = now.minus(24, ChronoUnit.HOURS);
    var summary = analyticsService.getSummary();

    Map<String, Object> tenants = new LinkedHashMap<>();
    tenants.put("total", summary.tenants().total());
    tenants.put("by_plan", summary.tenants().byPlan());
    tenants.put("by_status", summary.tenants().byStatus());
    tenants.put("by_region", summary.tenants().byRegion());

    Map<String, Object> policies = new LinkedHashMap<>();
    policies.put("total", summary.policies().total());
    policies.put("by_effect", summary.policies().byEffect());
    List<Map<String, Object>> topPermissions = new ArrayList<>();
    for (Object[] row : policyRepo.countActiveGroupByPermissionCode()) {
      topPermissions.add(Map.of("permission", row[0], "count", ((Number) row[1]).longValue()));
    }
    policies.put("top_permissions", topPermissions.stream().limit(10).toList());

    Map<String, Object> flags = new LinkedHashMap<>();
    flags.put("total", summary.flags().total());
    flags.put("enabled", summary.flags().enabled());
    flags.put("disabled", summary.flags().disabled());
    Object[] rollout = flagRepo.rolloutStats();
    if (rollout != null && rollout.length >= 2) {
      double avgRollout = rollout[0] instanceof Number n ? n.doubleValue() : 0;
      long partialCount = rollout[1] instanceof Number n ? n.longValue() : 0;
      flags.put("rollout", Map.of("avg_percent", avgRollout, "partial_rollout_count", partialCount));
    }

    List<Map<String, Object>> topActions = new ArrayList<>();
    for (Object[] row : auditRepo.topActionsSince(since24h, 10)) {
      topActions.add(Map.of("action", row[0], "count", ((Number) row[1]).longValue()));
    }
    List<Map<String, Object>> topActors = new ArrayList<>();
    for (Object[] row : auditRepo.topActorsSince(since24h, 10)) {
      topActors.add(Map.of("actor", row[0], "count", ((Number) row[1]).longValue()));
    }
    Map<String, Object> audit = new LinkedHashMap<>();
    audit.put("last_24h_count", summary.audit().last24h());
    audit.put("last_7d_count", summary.audit().last7d());
    audit.put("top_actions_24h", topActions);
    audit.put("top_actors_24h", topActors);

    long uptimeSeconds = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
    Map<String, Object> health = Map.of(
        "uptime_seconds", uptimeSeconds,
        "version", appVersion,
        "status", "up");

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("generated_at", now.toString());
    result.put("tenants", tenants);
    result.put("policies", policies);
    result.put("feature_flags", flags);
    result.put("audit_activity", audit);
    result.put("api_surface", API_SURFACE);
    result.put("system_health", health);
    return result;
  }

  @Transactional(readOnly = true)
  public Optional<Map<String, Object>> buildTenantDocs(UUID tenantId) {
    return tenantUseCase.getById(tenantId).map(tenant -> {
      Instant now = Instant.now();
      Instant since24h = now.minus(24, ChronoUnit.HOURS);

      Map<String, Object> tenantInfo = new LinkedHashMap<>();
      tenantInfo.put("id", tenant.getId().toString());
      tenantInfo.put("name", tenant.getName());
      tenantInfo.put("plan", tenant.getPlan());
      tenantInfo.put("region", tenant.getRegion());
      tenantInfo.put("status", tenant.getStatus().name());

      List<Map<String, Object>> policies = policyService
          .getApplicablePolicies(tenant.getPlan(), tenant.getRegion())
          .stream()
          .map(p -> Map.<String, Object>of(
              "id", p.getId().toString(),
              "permission_code", p.getPermissionCode(),
              "effect", p.getEffect().name(),
              "allowed_plans", p.getAllowedPlans(),
              "allowed_regions", p.getAllowedRegions()))
          .toList();
      tenantInfo.put("applicable_policies", policies);

      List<Map<String, Object>> flags = flagService.listByTenant(tenantId).stream()
          .map(f -> Map.<String, Object>of(
              "name", f.getName(),
              "enabled", f.isEnabled(),
              "rollout_percent", f.getRolloutPercent(),
              "allowed_roles", f.getAllowedRoles()))
          .toList();
      tenantInfo.put("feature_flags", flags);

      List<Map<String, Object>> topActions = new ArrayList<>();
      for (Object[] row : auditRepo.topActionsForTenantSince(since24h, tenantId, 5)) {
        topActions.add(Map.of("action", row[0], "count", ((Number) row[1]).longValue()));
      }
      List<Map<String, Object>> topActors = new ArrayList<>();
      for (Object[] row : auditRepo.topActorsForTenantSince(since24h, tenantId, 5)) {
        topActors.add(Map.of("actor", row[0], "count", ((Number) row[1]).longValue()));
      }
      Map<String, Object> audit = Map.of(
          "top_actions_24h", topActions,
          "top_actors_24h", topActors);

      Map<String, Object> result = new LinkedHashMap<>();
      result.put("generated_at", now.toString());
      result.put("tenant", tenantInfo);
      result.put("recent_audit_activity", audit);
      return result;
    });
  }
}
