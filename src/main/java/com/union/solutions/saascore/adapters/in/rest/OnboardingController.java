package com.union.solutions.saascore.adapters.in.rest;

import com.union.solutions.saascore.application.onboarding.OnboardingUseCase;
import com.union.solutions.saascore.application.port.TokenIssuer;
import com.union.solutions.saascore.domain.Tenant;
import com.union.solutions.saascore.domain.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/onboarding")
public class OnboardingController {

  private final OnboardingUseCase onboardingUseCase;
  private final TokenIssuer tokenIssuer;

  public OnboardingController(OnboardingUseCase onboardingUseCase, TokenIssuer tokenIssuer) {
    this.onboardingUseCase = onboardingUseCase;
    this.tokenIssuer = tokenIssuer;
  }

  @PostMapping("/signup")
  public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest request) {
    try {
      OnboardingUseCase.OnboardingResult result =
          onboardingUseCase.onboard(
              request.companyName(),
              request.plan(),
              request.region(),
              request.email(),
              request.name(),
              request.password());

      User user = result.user();
      Tenant tenant = result.tenant();
      String token =
          tokenIssuer.issue(
              user.getId().toString(),
              tenant.getId().toString(),
              user.getRoles(),
              List.of(),
              tenant.getPlan(),
              tenant.getRegion());

      return ResponseEntity.status(201)
          .body(
              Map.of(
                  "access_token",
                  token,
                  "token_type",
                  "Bearer",
                  "expires_in",
                  3600,
                  "tenant",
                  Map.of(
                      "id", tenant.getId(),
                      "name", tenant.getName(),
                      "plan", tenant.getPlan(),
                      "region", tenant.getRegion()),
                  "user",
                  Map.of(
                      "id", user.getId(),
                      "email", user.getEmail(),
                      "name", user.getName(),
                      "roles", user.getRoles())));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(409)
          .body(ProblemDetails.of(409, "Conflict", e.getMessage(), "/v1/onboarding/signup", null));
    }
  }

  public record SignupRequest(
      @NotBlank String companyName,
      @NotBlank String plan,
      String region,
      @NotBlank @Email String email,
      @NotBlank String name,
      @NotBlank @Size(min = 8) String password) {}
}
