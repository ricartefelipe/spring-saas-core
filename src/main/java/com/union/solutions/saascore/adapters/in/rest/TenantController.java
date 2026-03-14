package com.union.solutions.saascore.adapters.in.rest;

import com.union.solutions.saascore.application.abac.AbacEvaluator;
import com.union.solutions.saascore.application.tenant.TenantUseCase;
import com.union.solutions.saascore.domain.Tenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/tenants")
public class TenantController {

  private final TenantUseCase tenantUseCase;
  private final AbacEvaluator abacEvaluator;

  public TenantController(TenantUseCase tenantUseCase, AbacEvaluator abacEvaluator) {
    this.tenantUseCase = tenantUseCase;
    this.abacEvaluator = abacEvaluator;
  }

  @Operation(summary = "Create tenant", description = "Creates a new tenant with given plan and region")
  @ApiResponse(responseCode = "201", description = "Tenant created")
  @ApiResponse(responseCode = "403", description = "Access denied")
  @PostMapping
  public ResponseEntity<?> create(@Valid @RequestBody CreateTenantRequest request) {
    abacEvaluator.enforceOrThrow("tenants:write");
    Tenant t = tenantUseCase.create(request.name(), request.plan(), request.region());
    return ResponseEntity.status(201).body(TenantDto.from(t));
  }

  @Operation(summary = "List tenants", description = "Returns a cursor-paginated list of tenants with optional filtering")
  @ApiResponse(responseCode = "200", description = "Tenants listed successfully")
  @ApiResponse(responseCode = "403", description = "Access denied")
  @GetMapping
  public ResponseEntity<?> list(
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String plan,
      @RequestParam(required = false) String region,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false, defaultValue = "20") int limit) {
    abacEvaluator.enforceOrThrow("tenants:read");
    Tenant.TenantStatus statusEnum = null;
    if (status != null && !status.isBlank()) {
      try {
        statusEnum = Tenant.TenantStatus.valueOf(status);
      } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest()
            .body(
                ProblemDetails.of(
                    400, "Bad Request", "Invalid status: " + status, "/v1/tenants", null));
      }
    }

    int safeLimit = Math.max(1, Math.min(100, limit));
    String effectiveCursor = (cursor != null && !cursor.isBlank()) ? cursor : null;
    if (effectiveCursor == null) {
      // Sem cursor: retorna primeira página como CursorPage (evita serialização de Page)
      Instant cursorInstant = Instant.EPOCH;
      List<TenantDto> items =
          tenantUseCase
              .searchCursor(statusEnum, plan, region, name, cursorInstant, safeLimit)
              .stream()
              .map(TenantDto::from)
              .toList();
      boolean hasMore = items.size() == safeLimit;
      String nextCursor = buildNextCursor(items, hasMore);
      return ResponseEntity.ok(new CursorPage<>(items, nextCursor, hasMore));
    }

    Instant cursorInstant = decodeCursor(effectiveCursor);
    List<TenantDto> items =
        tenantUseCase
            .searchCursor(statusEnum, plan, region, name, cursorInstant, safeLimit)
            .stream()
            .map(TenantDto::from)
            .toList();
    boolean hasMore = items.size() == safeLimit;
    String nextCursor = buildNextCursor(items, hasMore);
    return ResponseEntity.ok(new CursorPage<>(items, nextCursor, hasMore));
  }

  private static String buildNextCursor(List<TenantDto> items, boolean hasMore) {
    if (!hasMore || items.isEmpty()) return null;
    Instant lastCreatedAt = items.get(items.size() - 1).createdAt();
    return lastCreatedAt != null ? encodeCursor(lastCreatedAt) : null;
  }

  @Operation(summary = "Get tenant by ID", description = "Returns a single tenant by UUID")
  @ApiResponse(responseCode = "200", description = "Tenant found")
  @ApiResponse(responseCode = "404", description = "Tenant not found")
  @ApiResponse(responseCode = "403", description = "Access denied")
  @GetMapping("/{id}")
  public ResponseEntity<?> getById(@PathVariable @NonNull UUID id) {
    abacEvaluator.enforceOrThrow("tenants:read");
    return tenantUseCase
        .getById(id)
        .map(t -> ResponseEntity.ok(TenantDto.from(t)))
        .orElse(ResponseEntity.notFound().build());
  }

  @Operation(summary = "Update tenant", description = "Partially updates tenant attributes")
  @ApiResponse(responseCode = "200", description = "Tenant updated")
  @ApiResponse(responseCode = "404", description = "Tenant not found")
  @ApiResponse(responseCode = "403", description = "Access denied")
  @PatchMapping("/{id}")
  public ResponseEntity<?> update(
      @PathVariable @NonNull UUID id, @RequestBody UpdateTenantRequest request) {
    abacEvaluator.enforceOrThrow("tenants:write");
    Tenant.TenantStatus statusEnum =
        request.status() != null ? Tenant.TenantStatus.valueOf(request.status()) : null;
    return tenantUseCase
        .update(id, request.name(), request.plan(), request.region(), statusEnum)
        .map(t -> ResponseEntity.ok(TenantDto.from(t)))
        .orElse(ResponseEntity.notFound().build());
  }

  @Operation(summary = "Delete tenant", description = "Soft-deletes a tenant by UUID")
  @ApiResponse(responseCode = "204", description = "Tenant deleted")
  @ApiResponse(responseCode = "404", description = "Tenant not found")
  @ApiResponse(responseCode = "403", description = "Access denied")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable @NonNull UUID id) {
    abacEvaluator.enforceOrThrow("tenants:write");
    return tenantUseCase.softDelete(id)
        ? ResponseEntity.noContent().build()
        : ResponseEntity.notFound().build();
  }

  private static String encodeCursor(Instant instant) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(instant.toString().getBytes());
  }

  private static Instant decodeCursor(String cursor) {
    return Instant.parse(new String(Base64.getUrlDecoder().decode(cursor)));
  }

  public record CreateTenantRequest(
      @NotBlank String name, @NotBlank String plan, @NotBlank String region) {}

  public record UpdateTenantRequest(String name, String plan, String region, String status) {}
}
