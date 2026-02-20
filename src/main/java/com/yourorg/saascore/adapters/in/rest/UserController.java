package com.yourorg.saascore.adapters.in.rest;

import com.yourorg.saascore.application.user.UserUseCase;
import com.yourorg.saascore.config.TenantContext;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/users")
public class UserController {

    private final UserUseCase userUseCase;

    public UserController(UserUseCase userUseCase) {
        this.userUseCase = userUseCase;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> create(
            @Valid @RequestBody CreateUserRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        UUID tenantId = TenantContext.getTenantId().orElse(null);
        if (tenantId == null) {
            return ResponseEntity.status(403).build();
        }
        UserUseCase.IdempotentResult result =
                userUseCase.createUser(tenantId, request.getEmail(), request.getPassword(), idempotencyKey);
        return ResponseEntity.status(result.status()).body(result.body());
    }

    @PostMapping("/{id}/roles/{role}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> assignRole(@PathVariable UUID id, @PathVariable String role) {
        boolean ok = userUseCase.assignRole(id, role);
        return ok ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }
}
