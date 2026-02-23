package com.union.solutions.saascore.adapters.in.rest;

import com.union.solutions.saascore.adapters.out.persistence.FeatureFlagEntity;
import com.union.solutions.saascore.application.service.FeatureFlagService;
import com.union.solutions.saascore.config.TenantContext;
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

  public FeatureFlagController(FeatureFlagService flagService) {
    this.flagService = flagService;
  }

  @PostMapping
  public ResponseEntity<?> create(
      @PathVariable UUID tenantId, @Valid @RequestBody CreateFlagRequest request) {
    enforceTenantAccess(tenantId);
    FeatureFlagEntity flag =
        flagService.create(
            tenantId,
            request.name(),
            request.enabled(),
            request.rolloutPercent(),
            request.allowedRoles());
    return ResponseEntity.status(201).body(FlagDto.from(flag));
  }

  @GetMapping
  public ResponseEntity<List<FlagDto>> list(@PathVariable UUID tenantId) {
    enforceTenantAccess(tenantId);
    List<FlagDto> flags = flagService.listByTenant(tenantId).stream().map(FlagDto::from).toList();
    return ResponseEntity.ok(flags);
  }

  @PatchMapping("/{flagName}")
  public ResponseEntity<?> update(
      @PathVariable UUID tenantId,
      @PathVariable String flagName,
      @RequestBody UpdateFlagRequest request) {
    enforceTenantAccess(tenantId);
    return flagService
        .update(
            tenantId, flagName, request.enabled(), request.rolloutPercent(), request.allowedRoles())
        .map(f -> ResponseEntity.ok(FlagDto.from(f)))
        .orElse(ResponseEntity.notFound().build());
  }

  @DeleteMapping("/{flagName}")
  public ResponseEntity<Void> delete(@PathVariable UUID tenantId, @PathVariable String flagName) {
    enforceTenantAccess(tenantId);
    return flagService.delete(tenantId, flagName)
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
    public static FlagDto from(FeatureFlagEntity e) {
      return new FlagDto(
          e.getId(),
          e.getTenantId(),
          e.getName(),
          e.isEnabled(),
          e.getRolloutPercent(),
          e.getAllowedRoles(),
          e.getCreatedAt(),
          e.getUpdatedAt());
    }
  }
}
