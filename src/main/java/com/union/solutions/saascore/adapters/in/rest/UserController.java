package com.union.solutions.saascore.adapters.in.rest;

import com.union.solutions.saascore.application.abac.AbacEvaluator;
import com.union.solutions.saascore.application.user.UserManagementUseCase;
import com.union.solutions.saascore.config.TenantContext;
import com.union.solutions.saascore.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

  @Operation(summary = "List users", description = "Returns users for the current tenant")
  @ApiResponse(responseCode = "200", description = "Users listed successfully")
  @ApiResponse(responseCode = "403", description = "Access denied")
  @GetMapping
  public ResponseEntity<?> list() {
    abacEvaluator.enforceOrThrow("users:read");
    UUID tenantId = requireTenantId();
    if (tenantId == null) return tenantRequired();

    List<UserDto> users = userUseCase.listByTenant(tenantId).stream().map(UserDto::from).toList();
    return ResponseEntity.ok(users);
  }

  @Operation(summary = "Get user by ID", description = "Returns a single user by its UUID")
  @ApiResponse(responseCode = "200", description = "User found")
  @ApiResponse(responseCode = "404", description = "User not found")
  @ApiResponse(responseCode = "403", description = "Access denied")
  @GetMapping("/{id}")
  public ResponseEntity<?> getById(@PathVariable @NonNull UUID id) {
    abacEvaluator.enforceOrThrow("users:read");
    UUID tenantId = requireTenantId();
    if (tenantId == null) return tenantRequired();

    return userUseCase
        .getById(id, tenantId)
        .map(u -> ResponseEntity.ok(UserDto.from(u)))
        .orElse(ResponseEntity.notFound().build());
  }

  @Operation(summary = "Update user", description = "Partially updates a user's name, roles or status")
  @ApiResponse(responseCode = "200", description = "User updated")
  @ApiResponse(responseCode = "404", description = "User not found")
  @ApiResponse(responseCode = "403", description = "Access denied")
  @PatchMapping("/{id}")
  public ResponseEntity<?> update(
      @PathVariable @NonNull UUID id, @RequestBody UpdateUserRequest request) {
    abacEvaluator.enforceOrThrow("users:write");
    UUID tenantId = requireTenantId();
    if (tenantId == null) return tenantRequired();

    User.UserStatus statusEnum = null;
    if (request.status() != null) {
      statusEnum = User.UserStatus.valueOf(request.status());
    }

    return userUseCase
        .update(id, tenantId, request.name(), request.roles(), statusEnum)
        .map(u -> ResponseEntity.ok(UserDto.from(u)))
        .orElse(ResponseEntity.notFound().build());
  }

  @Operation(summary = "Delete user", description = "Soft-deletes a user by UUID")
  @ApiResponse(responseCode = "204", description = "User deleted")
  @ApiResponse(responseCode = "404", description = "User not found")
  @ApiResponse(responseCode = "403", description = "Access denied")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable @NonNull UUID id) {
    abacEvaluator.enforceOrThrow("users:write");
    UUID tenantId = requireTenantId();
    if (tenantId == null) return ResponseEntity.status(400).build();

    return userUseCase.softDelete(id, tenantId)
        ? ResponseEntity.noContent().build()
        : ResponseEntity.notFound().build();
  }

  @Operation(summary = "Invite user", description = "Invites a new user to the current tenant")
  @ApiResponse(responseCode = "201", description = "User invited")
  @ApiResponse(responseCode = "409", description = "User already exists")
  @ApiResponse(responseCode = "403", description = "Access denied")
  @PostMapping("/invite")
  public ResponseEntity<?> invite(@Valid @RequestBody InviteUserRequest request) {
    abacEvaluator.enforceOrThrow("users:write");
    UUID tenantId = requireTenantId();
    if (tenantId == null) return tenantRequired();

    User invited = userUseCase.invite(tenantId, request.name(), request.email(), request.roles());
    return ResponseEntity.status(201).body(UserDto.from(invited));
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
