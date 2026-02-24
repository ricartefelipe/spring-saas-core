package com.union.solutions.saascore.adapters.in.rest;

import com.union.solutions.saascore.application.abac.AbacContext;
import com.union.solutions.saascore.application.abac.AbacEvaluator;
import com.union.solutions.saascore.application.abac.AbacResult;
import com.union.solutions.saascore.application.tenant.TenantUseCase;
import com.union.solutions.saascore.domain.Tenant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.lang.NonNull;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
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

  @PostMapping
  public ResponseEntity<?> create(@Valid @RequestBody CreateTenantRequest request) {
    AbacResult abac = abacEvaluator.evaluate(AbacContext.fromCurrentContext("tenants:write"));
    if (!abac.allowed())
      return ResponseEntity.status(403)
          .body(ProblemDetails.of(403, "Forbidden", abac.reason(), "/v1/tenants", null));
    Tenant t = tenantUseCase.create(request.name(), request.plan(), request.region());
    return ResponseEntity.status(201).body(TenantDto.from(t));
  }

  @GetMapping
  public ResponseEntity<?> list(
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String plan,
      @RequestParam(required = false) String region,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false, defaultValue = "20") int limit,
      @PageableDefault(size = 20) Pageable pageable) {
    Tenant.TenantStatus statusEnum = status != null ? Tenant.TenantStatus.valueOf(status) : null;

    if (cursor != null && !cursor.isBlank()) {
      Instant cursorInstant = decodeCursor(cursor);
      List<TenantDto> items =
          tenantUseCase.searchCursor(statusEnum, plan, region, name, cursorInstant, limit).stream()
              .map(TenantDto::from)
              .toList();
      boolean hasMore = items.size() == limit;
      String nextCursor = hasMore ? encodeCursor(items.get(items.size() - 1).createdAt()) : null;
      return ResponseEntity.ok(new CursorPage<>(items, nextCursor, hasMore));
    }

    Page<TenantDto> page =
        tenantUseCase.search(statusEnum, plan, region, name, pageable).map(TenantDto::from);
    return ResponseEntity.ok(page);
  }

  @GetMapping("/{id}")
  public ResponseEntity<TenantDto> getById(@PathVariable @NonNull UUID id) {
    return tenantUseCase
        .getById(id)
        .map(t -> ResponseEntity.ok(TenantDto.from(t)))
        .orElse(ResponseEntity.notFound().build());
  }

  @PatchMapping("/{id}")
  public ResponseEntity<?> update(@PathVariable @NonNull UUID id, @RequestBody UpdateTenantRequest request) {
    AbacResult abac = abacEvaluator.evaluate(AbacContext.fromCurrentContext("tenants:write"));
    if (!abac.allowed())
      return ResponseEntity.status(403)
          .body(ProblemDetails.of(403, "Forbidden", abac.reason(), "/v1/tenants/" + id, null));
    Tenant.TenantStatus statusEnum =
        request.status() != null ? Tenant.TenantStatus.valueOf(request.status()) : null;
    return tenantUseCase
        .update(id, request.name(), request.plan(), request.region(), statusEnum)
        .map(t -> ResponseEntity.ok(TenantDto.from(t)))
        .orElse(ResponseEntity.notFound().build());
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable @NonNull UUID id) {
    AbacResult abac = abacEvaluator.evaluate(AbacContext.fromCurrentContext("tenants:write"));
    if (!abac.allowed()) return ResponseEntity.status(403).build();
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
