package com.union.solutions.saascore.adapters.in.auth;

import com.union.solutions.saascore.application.auth.JwtTenantClaimsNormalizer;
import com.union.solutions.saascore.application.port.TokenIssuer;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/dev")
@ConditionalOnProperty(name = "app.dev.token-endpoint-enabled", havingValue = "true")
public class DevTokenController {

  private static final Logger log = LoggerFactory.getLogger(DevTokenController.class);

  private final TokenIssuer tokenIssuer;
  private final Environment environment;
  private boolean productionProfile;

  public DevTokenController(TokenIssuer tokenIssuer, Environment environment) {
    this.tokenIssuer = tokenIssuer;
    this.environment = environment;
  }

  @PostConstruct
  void init() {
    this.productionProfile =
        Arrays.stream(environment.getActiveProfiles())
            .anyMatch(p -> p.equalsIgnoreCase("prod") || p.equalsIgnoreCase("production"));
    if (productionProfile) {
      log.warn(
          "DevTokenController is loaded but production profile is active — "
              + "all requests will be refused. Set app.dev.token-endpoint-enabled=false in prod.");
    }
  }

  @PostMapping("/token")
  public ResponseEntity<?> issueDevToken(@Valid @RequestBody DevTokenRequest request) {
    if (productionProfile) {
      log.warn("Attempt to issue dev token in production profile refused (sub={})", request.sub());
      return ResponseEntity.status(403)
          .body(
              Map.of(
                  "error",
                  "dev_token_disabled",
                  "detail",
                  "Dev token endpoint is disabled in production profiles"));
    }
    String token =
        tokenIssuer.issue(
            request.sub(),
            request.tid(),
            request.roles(),
            request.perms(),
            JwtTenantClaimsNormalizer.plan(request.plan()),
            JwtTenantClaimsNormalizer.region(request.region()));
    return ResponseEntity.ok(
        Map.of("access_token", token, "token_type", "Bearer", "expires_in", 3600));
  }

  public record DevTokenRequest(
      @NotBlank String sub,
      @NotBlank String tid,
      List<String> roles,
      List<String> perms,
      String plan,
      String region) {}
}
