package com.union.solutions.saascore.adapters.in.rest;

import com.union.solutions.saascore.application.service.FeatureFlagService;
import com.union.solutions.saascore.application.service.PolicyService;
import com.union.solutions.saascore.application.tenant.TenantUseCase;
import com.union.solutions.saascore.domain.Policy;
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
  public ResponseEntity<Map<String, Object>> snapshot(
      @PathVariable @NonNull UUID id,
      @RequestParam(required = false) String include) {
    return tenantUseCase
        .getById(id)
        .map(
            t -> {
              Map<String, Object> body =
                  new java.util.LinkedHashMap<>(
                      Map.of(
                          "id", t.getId(),
                          "plan", t.getPlan(),
                          "region", t.getRegion(),
                          "status", t.getStatus().name()));
              if (include != null && !include.isBlank()) {
                String[] parts = include.toLowerCase().split(",");
                for (String part : parts) {
                  if ("policies".equals(part.trim())) {
                    List<Policy> policies =
                        policyService.getApplicablePolicies(t.getPlan(), t.getRegion());
                    body.put(
                        "policies",
                        policies.stream()
                            .map(PolicyController.PolicyDto::from)
                            .map(
                                dto ->
                                    Map.of(
                                        "id", dto.id(),
                                        "permission_code", dto.permissionCode(),
                                        "effect", dto.effect(),
                                        "allowed_plans", dto.allowedPlans(),
                                        "allowed_regions", dto.allowedRegions()))
                            .toList());
                  } else if ("flags".equals(part.trim())) {
                    body.put(
                        "flags",
                        flagService.listByTenant(id).stream()
                            .map(FeatureFlagController.FlagDto::from)
                            .toList());
                  }
                }
              }
              return ResponseEntity.ok(body);
            })
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/policies")
  public ResponseEntity<?> policies(@PathVariable @NonNull UUID id) {
    return tenantUseCase
        .getById(id)
        .map(
            t -> {
              List<Policy> policies =
                  policyService.getApplicablePolicies(t.getPlan(), t.getRegion());
              return ResponseEntity.ok(
                  policies.stream()
                      .map(PolicyController.PolicyDto::from)
                      .map(
                          dto ->
                              Map.of(
                                  "id", dto.id(),
                                  "permission_code", dto.permissionCode(),
                                  "effect", dto.effect(),
                                  "allowed_plans", dto.allowedPlans(),
                                  "allowed_regions", dto.allowedRegions()))
                      .toList());
            })
        .orElse(ResponseEntity.notFound().build());
  }
}
