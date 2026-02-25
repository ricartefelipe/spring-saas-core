package com.union.solutions.saascore.adapters.in.rest;

import com.union.solutions.saascore.adapters.out.persistence.PolicyEntity;
import com.union.solutions.saascore.application.service.FeatureFlagService;
import com.union.solutions.saascore.application.service.PolicyService;
import com.union.solutions.saascore.application.tenant.TenantUseCase;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/tenants/{id}")
public class TenantSnapshotController {

  private final TenantUseCase tenantUseCase;
  private final PolicyService policyService;
  private final FeatureFlagService flagService;

  public TenantSnapshotController(
      TenantUseCase tenantUseCase, PolicyService policyService, FeatureFlagService flagService) {
    this.tenantUseCase = tenantUseCase;
    this.policyService = policyService;
    this.flagService = flagService;
  }

  @GetMapping("/snapshot")
  public ResponseEntity<Map<String, Object>> snapshot(@PathVariable @NonNull UUID id) {
    return tenantUseCase
        .getById(id)
        .map(
            t ->
                ResponseEntity.ok(
                    Map.<String, Object>of(
                        "id", t.getId(),
                        "plan", t.getPlan(),
                        "region", t.getRegion(),
                        "status", t.getStatus().name())))
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/policies")
  public ResponseEntity<?> policies(@PathVariable @NonNull UUID id) {
    return tenantUseCase
        .getById(id)
        .map(
            t -> {
              List<PolicyEntity> policies =
                  policyService.getApplicablePolicies(t.getPlan(), t.getRegion());
              return ResponseEntity.ok(
                  policies.stream()
                      .map(
                          p ->
                              Map.of(
                                  "id", p.getId(),
                                  "permission_code", p.getPermissionCode(),
                                  "effect", p.getEffect().name(),
                                  "allowed_plans", p.getAllowedPlans(),
                                  "allowed_regions", p.getAllowedRegions()))
                      .toList());
            })
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/flags")
  public ResponseEntity<List<FeatureFlagController.FlagDto>> flags(@PathVariable @NonNull UUID id) {
    return tenantUseCase
        .getById(id)
        .map(
            t ->
                ResponseEntity.ok(
                    flagService.listByTenant(id).stream()
                        .map(FeatureFlagController.FlagDto::from)
                        .toList()))
        .orElse(ResponseEntity.notFound().build());
  }
}
