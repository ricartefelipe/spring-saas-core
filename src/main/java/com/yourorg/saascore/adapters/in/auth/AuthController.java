package com.yourorg.saascore.adapters.in.auth;

import com.yourorg.saascore.application.auth.AuthUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    private final AuthUseCase authUseCase;

    public AuthController(AuthUseCase authUseCase) {
        this.authUseCase = authUseCase;
    }

    @PostMapping("/token")
    public ResponseEntity<?> token(@Valid @RequestBody TokenRequest request) {
        return authUseCase
                .login(request.getUsername(), request.getPassword())
                .map(
                        r ->
                                ResponseEntity.ok(
                                        new TokenResponse(r.token(), 3600)))
                .orElse(ResponseEntity.status(401).build());
    }
}
