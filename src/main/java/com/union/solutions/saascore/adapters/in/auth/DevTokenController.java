package com.union.solutions.saascore.adapters.in.auth;

import com.union.solutions.saascore.application.port.TokenIssuer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/dev")
@ConditionalOnProperty(name = "app.dev.token-endpoint-enabled", havingValue = "true")
public class DevTokenController {

  private final TokenIssuer tokenIssuer;

  public DevTokenController(TokenIssuer tokenIssuer) {
    this.tokenIssuer = tokenIssuer;
  }

  @PostMapping("/token")
  public ResponseEntity<Map<String, Object>> issueDevToken(
      @Valid @RequestBody DevTokenRequest request) {
    String token =
        tokenIssuer.issue(
            request.sub(),
            request.tid(),
            request.roles(),
            request.perms(),
            request.plan(),
            request.region());
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
