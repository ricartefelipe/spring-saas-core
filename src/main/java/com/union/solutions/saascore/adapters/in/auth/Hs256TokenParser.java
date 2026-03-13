package com.union.solutions.saascore.adapters.in.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.auth.mode", havingValue = "hs256", matchIfMissing = true)
public class Hs256TokenParser implements TokenParser {

  private static final Logger log = LoggerFactory.getLogger(Hs256TokenParser.class);

  private final SecretKey currentKey;
  private final SecretKey previousKey;

  public Hs256TokenParser(
      SecretKey jwtSecretKey,
      @Value("${app.auth.jwt.hs256-secret-previous:}") String previousSecret) {
    this.currentKey = jwtSecretKey;
    this.previousKey =
        (previousSecret != null && !previousSecret.isBlank())
            ? new SecretKeySpec(previousSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256")
            : null;
  }

  @Override
  public Optional<TokenParseResult> parse(String token) {
    try {
      Claims c = Jwts.parser().verifyWith(currentKey).build().parseSignedClaims(token).getPayload();
      return Optional.of(TokenParseResult.current(toClaims(c)));
    } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
      if (previousKey != null) {
        try {
          Claims c =
              Jwts.parser().verifyWith(previousKey).build().parseSignedClaims(token).getPayload();
          log.warn(
              "JWT verified with previous/rotated key — sub={} tid={}; rotation in progress",
              c.getSubject(),
              c.get("tid", String.class));
          return Optional.of(TokenParseResult.previous(toClaims(c)));
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException ignored) {
          // both keys failed
        }
      }
      return Optional.empty();
    }
  }

  private TokenClaims toClaims(Claims c) {
    String sub = c.getSubject();
    String tid = c.get("tid", String.class);
    String plan = c.get("plan", String.class);
    String region = c.get("region", String.class);
    @SuppressWarnings("unchecked")
    List<String> roles =
        c.get("roles") != null ? (List<String>) c.get("roles") : Collections.emptyList();
    @SuppressWarnings("unchecked")
    List<String> perms =
        c.get("perms") != null ? (List<String>) c.get("perms") : Collections.emptyList();
    return new TokenClaims(
        sub, tid, roles, perms, plan != null ? plan : "", region != null ? region : "");
  }
}
