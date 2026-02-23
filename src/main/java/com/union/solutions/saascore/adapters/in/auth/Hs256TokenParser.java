package com.union.solutions.saascore.adapters.in.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.auth.mode", havingValue = "hs256", matchIfMissing = true)
public class Hs256TokenParser implements TokenParser {

  private final SecretKey key;

  public Hs256TokenParser(SecretKey jwtSecretKey) {
    this.key = jwtSecretKey;
  }

  @Override
  public Optional<TokenClaims> parse(String token) {
    try {
      Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
      return Optional.of(toClaims(c));
    } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
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
