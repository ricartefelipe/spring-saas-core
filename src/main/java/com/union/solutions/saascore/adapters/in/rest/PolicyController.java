package com.union.solutions.saascore.adapters.in.rest;

import com.union.solutions.saascore.application.abac.AbacEvaluator;
import com.union.solutions.saascore.application.service.PolicyService;
import com.union.solutions.saascore.domain.Policy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/policies")
public class PolicyController {

  private final PolicyService policyService;
  private final AbacEvaluator abacEvaluator;

  public PolicyController(PolicyService policyService, AbacEvaluator abacEvaluator) {
    this.policyService = policyService;
    this.abacEvaluator = abacEvaluator;
  }

  @Operation(summary = "Create policy", description = "Creates a new ABAC policy")
  @ApiResponse(responseCode = "201", description = "Policy created")
  @ApiResponse(responseCode = "403", description = "Access denied")
  @PostMapping
  public ResponseEntity<?> create(@Valid @RequestBody CreatePolicyRequest request) {
    abacEvaluator.enforceOrThrow("policies:write");
    Policy policy =
        policyService.create(
            request.permissionCode(),
            request.effect(),
            request.allowedPlans(),
            request.allowedRegions(),
            request.enabled(),
            request.notes());
    return ResponseEntity.status(201).body(PolicyDto.from(policy));
  }

  @Operation(summary = "List policies", description = "Returns a paginated list of ABAC policies with optional filtering")
  @ApiResponse(responseCode = "200", description = "Policies listed successfully")
  @ApiResponse(responseCode = "403", description = "Access denied")
  @GetMapping
  public ResponseEntity<?> list(
      @RequestParam(required = false) String permissionCode,
      @RequestParam(required = false) Policy.Effect effect,
      @RequestParam(required = false) Boolean enabled,
      @PageableDefault(size = 20) Pageable pageable) {
    abacEvaluator.enforceOrThrow("policies:read");
    var page = policyService.search(permissionCode, effect, enabled, pageable).map(PolicyDto::from);
    return ResponseEntity.ok(new PolicyPageResponse(page.getContent(), page.getTotalElements()));
  }

  @Operation(summary = "Get policy by ID", description = "Returns a single ABAC policy by UUID")
  @ApiResponse(responseCode = "200", description = "Policy found")
  @ApiResponse(responseCode = "404", description = "Policy not found")
  @ApiResponse(responseCode = "403", description = "Access denied")
  @GetMapping("/{id}")
  public ResponseEntity<?> getById(@PathVariable UUID id) {
    abacEvaluator.enforceOrThrow("policies:read");
    return policyService
        .getById(id)
        .map(p -> ResponseEntity.ok(PolicyDto.from(p)))
        .orElse(ResponseEntity.notFound().build());
  }

  @Operation(summary = "Update policy", description = "Partially updates an ABAC policy")
  @ApiResponse(responseCode = "200", description = "Policy updated")
  @ApiResponse(responseCode = "404", description = "Policy not found")
  @ApiResponse(responseCode = "403", description = "Access denied")
  @PatchMapping("/{id}")
  public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody UpdatePolicyRequest request) {
    abacEvaluator.enforceOrThrow("policies:write");
    return policyService
        .update(
            id,
            request.permissionCode(),
            request.effect(),
            request.allowedPlans(),
            request.allowedRegions(),
            request.enabled(),
            request.notes())
        .map(p -> ResponseEntity.ok(PolicyDto.from(p)))
        .orElse(ResponseEntity.notFound().build());
  }

  @Operation(summary = "Delete policy", description = "Soft-deletes an ABAC policy by UUID")
  @ApiResponse(responseCode = "204", description = "Policy deleted")
  @ApiResponse(responseCode = "404", description = "Policy not found")
  @ApiResponse(responseCode = "403", description = "Access denied")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    abacEvaluator.enforceOrThrow("policies:write");
    return policyService.softDelete(id)
        ? ResponseEntity.noContent().build()
        : ResponseEntity.notFound().build();
  }

  public record CreatePolicyRequest(
      @NotBlank String permissionCode,
      @NotNull Policy.Effect effect,
      List<String> allowedPlans,
      List<String> allowedRegions,
      boolean enabled,
      String notes) {}

  public record UpdatePolicyRequest(
      String permissionCode,
      Policy.Effect effect,
      List<String> allowedPlans,
      List<String> allowedRegions,
      Boolean enabled,
      String notes) {}

  /** DTO para evitar serialização direta de Spring Page (Sort etc.). */
  public record PolicyPageResponse(List<PolicyDto> content, long totalElements) {}

  public record PolicyDto(
      UUID id,
      String permissionCode,
      String effect,
      String allowedPlans,
      String allowedRegions,
      boolean enabled,
      String notes,
      java.time.Instant createdAt,
      java.time.Instant updatedAt) {
    public static PolicyDto from(Policy p) {
      return new PolicyDto(
          p.getId(),
          p.getPermissionCode(),
          p.getEffect().name(),
          toJson(p.getAllowedPlans()),
          toJson(p.getAllowedRegions()),
          p.isEnabled(),
          p.getNotes(),
          p.getCreatedAt(),
          p.getUpdatedAt());
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
