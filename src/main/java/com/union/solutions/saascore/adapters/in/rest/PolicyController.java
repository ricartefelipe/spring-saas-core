package com.union.solutions.saascore.adapters.in.rest;

import com.union.solutions.saascore.adapters.out.persistence.PolicyEntity;
import com.union.solutions.saascore.application.abac.AbacContext;
import com.union.solutions.saascore.application.abac.AbacEvaluator;
import com.union.solutions.saascore.application.abac.AbacResult;
import com.union.solutions.saascore.application.service.PolicyService;
import com.union.solutions.saascore.domain.Policy;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
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

  @PostMapping
  public ResponseEntity<?> create(@Valid @RequestBody CreatePolicyRequest request) {
    AbacResult abac = abacEvaluator.evaluate(AbacContext.fromCurrentContext("policies:write"));
    if (!abac.allowed())
      return ResponseEntity.status(403)
          .body(ProblemDetails.of(403, "Forbidden", abac.reason(), "/v1/policies", null));
    PolicyEntity entity =
        policyService.create(
            request.permissionCode(),
            request.effect(),
            request.allowedPlans(),
            request.allowedRegions(),
            request.enabled(),
            request.notes());
    return ResponseEntity.status(201).body(PolicyDto.from(entity));
  }

  @GetMapping
  public ResponseEntity<Page<PolicyDto>> list(
      @RequestParam(required = false) String permissionCode,
      @RequestParam(required = false) Policy.Effect effect,
      @RequestParam(required = false) Boolean enabled,
      @PageableDefault(size = 20) Pageable pageable) {
    Page<PolicyDto> page =
        policyService.search(permissionCode, effect, enabled, pageable).map(PolicyDto::from);
    return ResponseEntity.ok(page);
  }

  @GetMapping("/{id}")
  public ResponseEntity<PolicyDto> getById(@PathVariable UUID id) {
    return policyService
        .getById(id)
        .map(p -> ResponseEntity.ok(PolicyDto.from(p)))
        .orElse(ResponseEntity.notFound().build());
  }

  @PatchMapping("/{id}")
  public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody UpdatePolicyRequest request) {
    AbacResult abac = abacEvaluator.evaluate(AbacContext.fromCurrentContext("policies:write"));
    if (!abac.allowed())
      return ResponseEntity.status(403)
          .body(ProblemDetails.of(403, "Forbidden", abac.reason(), "/v1/policies/" + id, null));
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

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    AbacResult abac = abacEvaluator.evaluate(AbacContext.fromCurrentContext("policies:write"));
    if (!abac.allowed()) return ResponseEntity.status(403).build();
    return policyService.delete(id)
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
    public static PolicyDto from(PolicyEntity e) {
      return new PolicyDto(
          e.getId(),
          e.getPermissionCode(),
          e.getEffect().name(),
          e.getAllowedPlans(),
          e.getAllowedRegions(),
          e.isEnabled(),
          e.getNotes(),
          e.getCreatedAt(),
          e.getUpdatedAt());
    }
  }
}
