package com.union.solutions.saascore.adapters.in.rest;

import com.union.solutions.saascore.application.user.EmailAlreadyExistsException;
import com.union.solutions.saascore.application.user.UserUseCase;
import com.union.solutions.saascore.domain.User;
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

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            User user = userUseCase.register(
                    request.email(), request.name(), request.password(),
                    request.tenantId(), request.roles());
            return ResponseEntity.status(201).body(new UserResponse(
                    user.getId(), user.getEmail(), user.getName(),
                    user.getTenantId(), user.getRoles(), user.getStatus().name(), user.getCreatedAt().toString()));
        } catch (EmailAlreadyExistsException e) {
            return ResponseEntity.status(409)
                    .body(ProblemDetails.of(409, "Conflict", e.getMessage(), "/v1/auth/register", null));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        return userUseCase.authenticate(request.email(), request.password())
                .<ResponseEntity<?>>map(result -> ResponseEntity.ok(Map.of(
                        "access_token", result.accessToken(),
                        "token_type", "Bearer",
                        "expires_in", 3600,
                        "user", new UserResponse(
                                result.user().getId(), result.user().getEmail(), result.user().getName(),
                                result.user().getTenantId(), result.user().getRoles(),
                                result.user().getStatus().name(), result.user().getCreatedAt().toString()))))
                .orElseGet(() -> ResponseEntity.status(401)
                        .body(ProblemDetails.of(401, "Unauthorized", "Invalid email or password", "/v1/auth/login", null)));
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<?> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        userUseCase.requestPasswordReset(request.email());
        return ResponseEntity.ok(Map.of("message", "If the email exists, a reset link has been sent"));
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<?> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        boolean success = userUseCase.resetPassword(request.tokenId(), request.token(), request.newPassword());
        if (success) {
            return ResponseEntity.ok(Map.of("message", "Password has been reset successfully"));
        }
        return ResponseEntity.status(400)
                .body(ProblemDetails.of(400, "Bad Request", "Invalid or expired reset token", "/v1/auth/password-reset/confirm", null));
    }

    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank String name,
            @NotBlank @Size(min = 8) String password,
            UUID tenantId,
            List<String> roles) {}

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password) {}

    public record PasswordResetRequest(
            @NotBlank @Email String email) {}

    public record PasswordResetConfirmRequest(
            UUID tokenId,
            @NotBlank String token,
            @NotBlank @Size(min = 8) String newPassword) {}

    public record UserResponse(UUID id, String email, String name, UUID tenantId,
                               List<String> roles, String status, String createdAt) {}
}
