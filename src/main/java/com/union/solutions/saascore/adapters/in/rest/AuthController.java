package com.union.solutions.saascore.adapters.in.rest;

import com.union.solutions.saascore.application.user.UserUseCase;
import com.union.solutions.saascore.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {

  private final UserUseCase userUseCase;

  public AuthController(UserUseCase userUseCase) {
    this.userUseCase = userUseCase;
  }

  @Operation(
      summary = "Register user",
      description = "Creates a new user account with email and password")
  @ApiResponse(responseCode = "201", description = "User registered")
  @ApiResponse(responseCode = "409", description = "Email already registered")
  @PostMapping("/register")
  public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
    User user =
        userUseCase.register(
            request.email(),
            request.name(),
            request.password(),
            request.tenantId(),
            request.roles());
    return ResponseEntity.status(201)
        .body(
            new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getTenantId(),
                user.getRoles(),
                user.getStatus().name(),
                user.getCreatedAt().toString()));
  }

  @Operation(summary = "Login", description = "Authenticates a user and returns a JWT access token")
  @ApiResponse(responseCode = "200", description = "Login successful")
  @ApiResponse(responseCode = "401", description = "Invalid credentials")
  @PostMapping("/login")
  public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
    return userUseCase
        .authenticate(request.email(), request.password())
        .<ResponseEntity<?>>map(
            result ->
                ResponseEntity.ok(
                    Map.of(
                        "access_token",
                        result.accessToken(),
                        "token_type",
                        "Bearer",
                        "expires_in",
                        3600,
                        "user",
                        new UserResponse(
                            result.user().getId(),
                            result.user().getEmail(),
                            result.user().getName(),
                            result.user().getTenantId(),
                            result.user().getRoles(),
                            result.user().getStatus().name(),
                            result.user().getCreatedAt().toString()))))
        .orElseGet(
            () ->
                ResponseEntity.status(401)
                    .body(
                        ProblemDetails.of(
                            401,
                            "Unauthorized",
                            "Invalid email or password",
                            "/v1/auth/login",
                            null)));
  }

  @Operation(
      summary = "Request password reset",
      description = "Sends a password reset link to the given email if it exists")
  @ApiResponse(responseCode = "200", description = "Reset email sent (if email exists)")
  @PostMapping("/password-reset/request")
  public ResponseEntity<?> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
    userUseCase.requestPasswordReset(request.email());
    return ResponseEntity.ok(Map.of("message", "If the email exists, a reset link has been sent"));
  }

  @Operation(
      summary = "Confirm password reset",
      description = "Resets the password using a valid reset token")
  @ApiResponse(responseCode = "200", description = "Password reset successful")
  @ApiResponse(responseCode = "400", description = "Invalid or expired token")
  @PostMapping("/password-reset/confirm")
  public ResponseEntity<?> confirmPasswordReset(
      @Valid @RequestBody PasswordResetConfirmRequest request) {
    boolean success =
        userUseCase.resetPassword(request.tokenId(), request.token(), request.newPassword());
    if (success) {
      return ResponseEntity.ok(Map.of("message", "Password has been reset successfully"));
    }
    return ResponseEntity.status(400)
        .body(
            ProblemDetails.of(
                400,
                "Bad Request",
                "Invalid or expired reset token",
                "/v1/auth/password-reset/confirm",
                null));
  }

  public record RegisterRequest(
      @NotBlank @Email String email,
      @NotBlank String name,
      @NotBlank @Size(min = 8) String password,
      UUID tenantId,
      List<String> roles) {}

  public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

  public record PasswordResetRequest(@NotBlank @Email String email) {}

  public record PasswordResetConfirmRequest(
      UUID tokenId, @NotBlank String token, @NotBlank @Size(min = 8) String newPassword) {}

  public record UserResponse(
      UUID id,
      String email,
      String name,
      UUID tenantId,
      List<String> roles,
      String status,
      String createdAt) {}
}
