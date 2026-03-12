package com.union.solutions.saascore.adapters.in.rest;

import com.union.solutions.saascore.application.abac.AbacContext;
import com.union.solutions.saascore.application.abac.AbacEvaluator;
import com.union.solutions.saascore.application.abac.AbacResult;
import com.union.solutions.saascore.application.user.UserAlreadyExistsException;
import com.union.solutions.saascore.application.user.UserManagementUseCase;
import com.union.solutions.saascore.config.TenantContext;
import com.union.solutions.saascore.domain.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/users")
public class UserController {

  private final UserManagementUseCase userUseCase;
  private final AbacEvaluator abacEvaluator;

  public UserController(UserManagementUseCase userUseCase, AbacEvaluator abacEvaluator) {
    this.userUseCase = userUseCase;
    this.abacEvaluator = abacEvaluator;
  }

  @GetMapping
  public ResponseEntity<?> list() {
    AbacResult abac = abacEvaluator.evaluate(AbacContext.fromCurrentContext("users:read"));
    if (!abac.allowed())
      return ResponseEntity.status(403)
          .body(ProblemDetails.of(403, "Forbidden", abac.reason(), "/v1/users", null));

    UUID tenantId = requireTenantId();
    if (tenantId == null) return tenantRequired();

    List<UserDto> users =
        userUseCase.listByTenant(tenantId).stream().map(UserDto::from).toList();
    return ResponseEntity.ok(users);
  }

  @GetMapping("/{id}")
  public ResponseEntity<?> getById(@PathVariable @NonNull UUID id) {
    AbacResult abac = abacEvaluator.evaluate(AbacContext.fromCurrentContext("users:read"));
    if (!abac.allowed())
      return ResponseEntity.status(403)
          .body(ProblemDetails.of(403, "Forbidden", abac.reason(), "/v1/users/" + id, null));

    UUID tenantId = requireTenantId();
    if (tenantId == null) return tenantRequired();

    return userUseCase
        .getById(id, tenantId)
        .map(u -> ResponseEntity.ok(UserDto.from(u)))
        .orElse(ResponseEntity.notFound().build());
  }

  @PatchMapping("/{id}")
  public ResponseEntity<?> update(
      @PathVariable @NonNull UUID id, @RequestBody UpdateUserRequest request) {
    AbacResult abac = abacEvaluator.evaluate(AbacContext.fromCurrentContext("users:write"));
    if (!abac.allowed())
      return ResponseEntity.status(403)
          .body(ProblemDetails.of(403, "Forbidden", abac.reason(), "/v1/users/" + id, null));

    UUID tenantId = requireTenantId();
    if (tenantId == null) return tenantRequired();

    User.UserStatus statusEnum = null;
    if (request.status() != null) {
      try {
        statusEnum = User.UserStatus.valueOf(request.status());
      } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest()
            .body(
                ProblemDetails.of(
                    400,
                    "Bad Request",
                    "Invalid status: " + request.status(),
                    "/v1/users/" + id,
                    null));
      }
    }

    return userUseCase
        .update(id, tenantId, request.name(), request.roles(), statusEnum)
        .map(u -> ResponseEntity.ok(UserDto.from(u)))
        .orElse(ResponseEntity.notFound().build());
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable @NonNull UUID id) {
    AbacResult abac = abacEvaluator.evaluate(AbacContext.fromCurrentContext("users:write"));
    if (!abac.allowed()) return ResponseEntity.status(403).build();

    UUID tenantId = requireTenantId();
    if (tenantId == null) return ResponseEntity.status(400).build();

    return userUseCase.softDelete(id, tenantId)
        ? ResponseEntity.noContent().build()
        : ResponseEntity.notFound().build();
  }

  @PostMapping("/invite")
  public ResponseEntity<?> invite(@Valid @RequestBody InviteUserRequest request) {
    AbacResult abac = abacEvaluator.evaluate(AbacContext.fromCurrentContext("users:write"));
    if (!abac.allowed())
      return ResponseEntity.status(403)
          .body(ProblemDetails.of(403, "Forbidden", abac.reason(), "/v1/users/invite", null));

    UUID tenantId = requireTenantId();
    if (tenantId == null) return tenantRequired();

    try {
      User invited = userUseCase.invite(tenantId, request.name(), request.email(), request.roles());
      return ResponseEntity.status(201).body(UserDto.from(invited));
    } catch (UserAlreadyExistsException e) {
      return ResponseEntity.status(409)
          .body(
              ProblemDetails.of(
                  409, "Conflict", e.getMessage(), "/v1/users/invite", null));
    }
  }

  private static UUID requireTenantId() {
    return TenantContext.getTenantId().orElse(null);
  }

  private static ResponseEntity<?> tenantRequired() {
    return ResponseEntity.badRequest()
        .body(ProblemDetails.of(400, "Bad Request", "Tenant ID is required", "/v1/users", null));
  }

  public record UserDto(
      UUID id,
      UUID tenantId,
      String name,
      String email,
      List<String> roles,
      String status,
      Instant createdAt,
      Instant updatedAt) {

    public static UserDto from(User u) {
      return new UserDto(
          u.getId(),
          u.getTenantId(),
          u.getName(),
          u.getEmail(),
          u.getRoles(),
          u.getStatus().name(),
          u.getCreatedAt(),
          u.getUpdatedAt());
    }
  }

  public record UpdateUserRequest(String name, List<String> roles, String status) {}

  public record InviteUserRequest(
      @NotBlank String name, @NotBlank @Email String email, List<String> roles) {}
}
