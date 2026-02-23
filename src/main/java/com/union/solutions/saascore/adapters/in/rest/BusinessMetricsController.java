package com.union.solutions.saascore.adapters.in.rest;

import com.union.solutions.saascore.adapters.out.persistence.TenantJpaRepository;
import com.union.solutions.saascore.application.service.FeatureFlagService;
import com.union.solutions.saascore.application.service.PolicyService;
import com.union.solutions.saascore.domain.Tenant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/metrics/business")
public class BusinessMetricsController {

  private final TenantJpaRepository tenantRepo;
  private final PolicyService policyService;
  private final FeatureFlagService flagService;

  public BusinessMetricsController(
      TenantJpaRepository tenantRepo, PolicyService policyService, FeatureFlagService flagService) {
    this.tenantRepo = tenantRepo;
    this.policyService = policyService;
    this.flagService = flagService;
  }

  @GetMapping
  public ResponseEntity<Map<String, Object>> metrics() {
    Map<String, Object> result = new LinkedHashMap<>();

    long activeCount = tenantRepo.countByStatus(Tenant.TenantStatus.ACTIVE);
    long suspendedCount = tenantRepo.countByStatus(Tenant.TenantStatus.SUSPENDED);
    long deletedCount = tenantRepo.countByStatus(Tenant.TenantStatus.DELETED);
    result.put(
        "tenants",
        Map.of(
            "active", activeCount,
            "suspended", suspendedCount,
            "deleted", deletedCount,
            "total", activeCount + suspendedCount + deletedCount));

    List<Object[]> byPlanStatus = tenantRepo.countByPlanAndStatus();
    Map<String, Map<String, Long>> tenantsByPlan = new LinkedHashMap<>();
    for (Object[] row : byPlanStatus) {
      String plan = (String) row[0];
      String status = ((Tenant.TenantStatus) row[1]).name();
      long count = (Long) row[2];
      tenantsByPlan.computeIfAbsent(plan, k -> new LinkedHashMap<>()).put(status, count);
    }
    result.put("tenants_by_plan", tenantsByPlan);

    result.put("active_policies", policyService.countActive());
    result.put("active_flags", flagService.countActiveFlags());

    return ResponseEntity.ok(result);
  }
}
