package com.union.solutions.saascore.adapters.in.rest;

import com.union.solutions.saascore.application.abac.AbacContext;
import com.union.solutions.saascore.application.abac.AbacEvaluator;
import com.union.solutions.saascore.application.abac.AbacResult;
import com.union.solutions.saascore.application.port.UserRepository;
import com.union.solutions.saascore.application.service.FeatureFlagService;
import com.union.solutions.saascore.application.service.PolicyService;
import com.union.solutions.saascore.application.tenant.TenantUseCase;
import com.union.solutions.saascore.config.TenantContext;
import com.union.solutions.saascore.domain.Policy;
import java.time.Instant;
import java.util.LinkedHashMap;
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
  private final UserRepository userRepo;
  private final AbacEvaluator abacEvaluator;

  public TenantSnapshotController(
      TenantUseCase tenantUseCase,
      PolicyService policyService,
      FeatureFlagService flagService,
      UserRepository userRepo,
      AbacEvaluator abacEvaluator) {
    this.tenantUseCase = tenantUseCase;
    this.policyService = policyService;
    this.flagService = flagService;
    this.userRepo = userRepo;
    this.abacEvaluator = abacEvaluator;
  }

  @GetMapping("/snapshot")
  public ResponseEntity<?> snapshot(
      @PathVariable @NonNull UUID id, @RequestParam(required = false) String include) {
    AbacResult abac = abacEvaluator.evaluate(AbacContext.fromCurrentContext("tenants:read"));
    if (!abac.allowed())
      return ResponseEntity.status(403)
          .body(
              ProblemDetails.of(
                  403, "Forbidden", abac.reason(), "/v1/tenants/" + id + "/snapshot", null));
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

  @GetMapping("/health")
  public ResponseEntity<?> health(@PathVariable @NonNull UUID id) {
    AbacResult abac = abacEvaluator.evaluate(AbacContext.fromCurrentContext("tenants:read"));
    if (!abac.allowed())
      return ResponseEntity.status(403)
          .body(
              ProblemDetails.of(
                  403, "Forbidden", abac.reason(), "/v1/tenants/" + id + "/health", null));
    return tenantUseCase
        .getById(id)
        .map(
            t -> {
              Instant lastActivityAt = userRepo.findMaxLastLoginAtByTenantId(id).orElse(null);
              long activeUsersCount = userRepo.countActiveByTenantId(id);
              return ResponseEntity.ok(
                  Map.of(
                      "tenantId", id,
                      "lastActivityAt", lastActivityAt != null ? lastActivityAt.toString() : null,
                      "activeUsersCount", activeUsersCount));
            })
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/export")
  public ResponseEntity<?> export(@PathVariable @NonNull UUID id) {
    AbacResult abac = abacEvaluator.evaluate(AbacContext.fromCurrentContext("tenants:read"));
    if (!abac.allowed())
      return ResponseEntity.status(403)
          .body(
              ProblemDetails.of(
                  403, "Forbidden", abac.reason(), "/v1/tenants/" + id + "/export", null));
    if (TenantContext.getTenantId().filter(id::equals).isEmpty())
      return ResponseEntity.status(403)
          .body(
              ProblemDetails.of(
                  403,
                  "Forbidden",
                  "Can only export data for your own tenant",
                  "/v1/tenants/" + id + "/export",
                  null));
    return tenantUseCase
        .getById(id)
        .map(
            t -> {
              Map<String, Object> exportData = new LinkedHashMap<>();
              exportData.put(
                  "tenant",
                  Map.of(
                      "id", t.getId(),
                      "name", t.getName(),
                      "plan", t.getPlan(),
                      "region", t.getRegion(),
                      "status", t.getStatus().name(),
                      "createdAt", t.getCreatedAt().toString(),
                      "updatedAt", t.getUpdatedAt().toString()));
              List<Map<String, Object>> usersList =
                  userRepo.findByTenantId(id).stream()
                      .map(
                          u ->
                              Map.<String, Object>of(
                                  "id", u.getId(),
                                  "email", u.getEmail(),
                                  "name", u.getName(),
                                  "roles", u.getRoles() != null ? u.getRoles() : List.of(),
                                  "status", u.getStatus().name(),
                                  "createdAt", u.getCreatedAt().toString(),
                                  "updatedAt", u.getUpdatedAt().toString(),
                                  "lastLoginAt",
                                      u.getLastLoginAt() != null
                                          ? u.getLastLoginAt().toString()
                                          : (Object) null))
                      .toList();
              exportData.put("users", usersList);
              List<Policy> policies =
                  policyService.getApplicablePolicies(t.getPlan(), t.getRegion());
              exportData.put(
                  "policies",
                  policies.stream()
                      .map(
                          p ->
                              Map.of(
                                  "id", p.getId(),
                                  "permissionCode", p.getPermissionCode(),
                                  "effect", p.getEffect().name(),
                                  "allowedPlans", p.getAllowedPlans(),
                                  "allowedRegions", p.getAllowedRegions()))
                      .toList());
              exportData.put(
                  "featureFlags",
                  flagService.listByTenant(id).stream()
                      .map(
                          f ->
                              Map.of(
                                  "id", f.getId(),
                                  "name", f.getName(),
                                  "enabled", f.isEnabled()))
                      .toList());
              return ResponseEntity.ok(exportData);
            })
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/policies")
  public ResponseEntity<?> policies(@PathVariable @NonNull UUID id) {
    AbacResult abac = abacEvaluator.evaluate(AbacContext.fromCurrentContext("tenants:read"));
    if (!abac.allowed())
      return ResponseEntity.status(403)
          .body(
              ProblemDetails.of(
                  403, "Forbidden", abac.reason(), "/v1/tenants/" + id + "/policies", null));
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
