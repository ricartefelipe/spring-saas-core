package com.union.solutions.saascore.adapters.in.rest;

import com.union.solutions.saascore.application.abac.AbacEvaluator;
import com.union.solutions.saascore.application.service.FeatureFlagService;
import com.union.solutions.saascore.config.TenantContext;
import com.union.solutions.saascore.domain.FeatureFlag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/tenants/{tenantId}/flags")
public class FeatureFlagController {

  private final FeatureFlagService flagService;
  private final AbacEvaluator abacEvaluator;

  public FeatureFlagController(FeatureFlagService flagService, AbacEvaluator abacEvaluator) {
    this.flagService = flagService;
    this.abacEvaluator = abacEvaluator;
  }

  @Operation(
      summary = "Create feature flag",
      description = "Creates a new feature flag for the tenant")
  @ApiResponse(responseCode = "201", description = "Flag created")
  @ApiResponse(responseCode = "403", description = "Access denied")
  @PostMapping
  public ResponseEntity<?> create(
      @PathVariable UUID tenantId, @Valid @RequestBody CreateFlagRequest request) {
    abacEvaluator.enforceOrThrow("flags:write");
    enforceTenantAccess(tenantId);
    FeatureFlag flag =
        flagService.create(
            tenantId,
            request.name(),
            request.enabled(),
            request.rolloutPercent(),
            request.allowedRoles());
    return ResponseEntity.status(201).body(FlagDto.from(flag));
  }

  @Operation(
      summary = "List feature flags",
      description = "Returns all feature flags for the tenant")
  @ApiResponse(responseCode = "200", description = "Flags listed successfully")
  @ApiResponse(responseCode = "403", description = "Access denied")
  @GetMapping
  public ResponseEntity<?> list(@PathVariable UUID tenantId) {
    abacEvaluator.enforceOrThrow("flags:read");
    enforceTenantAccess(tenantId);
    List<FlagDto> flags = flagService.listByTenant(tenantId).stream().map(FlagDto::from).toList();
    return ResponseEntity.ok(flags);
  }

  @Operation(
      summary = "Update feature flag",
      description = "Partially updates a feature flag by name")
  @ApiResponse(responseCode = "200", description = "Flag updated")
  @ApiResponse(responseCode = "404", description = "Flag not found")
  @ApiResponse(responseCode = "403", description = "Access denied")
  @PatchMapping("/{flagName}")
  public ResponseEntity<?> update(
      @PathVariable UUID tenantId,
      @PathVariable String flagName,
      @RequestBody UpdateFlagRequest request) {
    abacEvaluator.enforceOrThrow("flags:write");
    enforceTenantAccess(tenantId);
    return flagService
        .update(
            tenantId, flagName, request.enabled(), request.rolloutPercent(), request.allowedRoles())
        .map(f -> ResponseEntity.ok(FlagDto.from(f)))
        .orElse(ResponseEntity.notFound().build());
  }

  @Operation(summary = "Delete feature flag", description = "Soft-deletes a feature flag by name")
  @ApiResponse(responseCode = "204", description = "Flag deleted")
  @ApiResponse(responseCode = "404", description = "Flag not found")
  @ApiResponse(responseCode = "403", description = "Access denied")
  @DeleteMapping("/{flagName}")
  public ResponseEntity<?> delete(@PathVariable UUID tenantId, @PathVariable String flagName) {
    abacEvaluator.enforceOrThrow("flags:write");
    enforceTenantAccess(tenantId);
    return flagService.softDelete(tenantId, flagName)
        ? ResponseEntity.noContent().build()
        : ResponseEntity.notFound().build();
  }

  private void enforceTenantAccess(UUID tenantId) {
    UUID ctxTenant = TenantContext.getTenantId().orElse(null);
    if (ctxTenant != null && !ctxTenant.equals(tenantId)) {
      throw new org.springframework.security.access.AccessDeniedException("Tenant mismatch");
    }
  }

  public record CreateFlagRequest(
      @NotBlank String name, boolean enabled, int rolloutPercent, List<String> allowedRoles) {}

  public record UpdateFlagRequest(
      Boolean enabled, Integer rolloutPercent, List<String> allowedRoles) {}

  public record FlagDto(
      UUID id,
      UUID tenantId,
      String name,
      boolean enabled,
      int rolloutPercent,
      String allowedRoles,
      Instant createdAt,
      Instant updatedAt) {
    public static FlagDto from(FeatureFlag f) {
      return new FlagDto(
          f.getId(),
          f.getTenantId(),
          f.getName(),
          f.isEnabled(),
          f.getRolloutPercent(),
          toJson(f.getAllowedRoles()),
          f.getCreatedAt(),
          f.getUpdatedAt());
    }

    private static String toJson(java.util.List<String> list) {
      if (list == null || list.isEmpty()) return "[]";
      try {
        return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(list);
      } catch (Exception e) {
        return "[]";
      }
    }
  }
}
