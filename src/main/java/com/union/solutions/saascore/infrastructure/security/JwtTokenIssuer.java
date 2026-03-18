package com.union.solutions.saascore.infrastructure.security;

import com.union.solutions.saascore.application.port.TokenIssuer;
import io.jsonwebtoken.Jwts;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenIssuer implements TokenIssuer {

  private final SecretKey secretKey;
  private final String issuer;
  private final long expirationSeconds;

  public JwtTokenIssuer(
      SecretKey secretKey,
      @Value("${app.auth.jwt.issuer}") String issuer,
      @Value("${app.auth.jwt.expiration-seconds:3600}") long expirationSeconds) {
    this.secretKey = secretKey;
    this.issuer = issuer;
    this.expirationSeconds = expirationSeconds;
  }

  @Override
  public String issue(
      String sub,
      String tid,
      List<String> roles,
      List<String> perms,
      String plan,
      String region,
      boolean mustChangePassword) {
    Instant now = Instant.now();
    Map<String, Object> claims = new LinkedHashMap<>();
    claims.put("tid", tid != null ? tid : "");
    claims.put("roles", roles != null ? roles : List.of());
    claims.put("perms", perms != null ? perms : List.of());
    claims.put("plan", plan != null ? plan : "");
    claims.put("region", region != null ? region : "");
    claims.put("mcp", mustChangePassword);
    return Jwts.builder()
        .issuer(issuer)
        .subject(sub)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusSeconds(expirationSeconds)))
        .id(UUID.randomUUID().toString())
        .claims(claims)
        .signWith(secretKey)
        .compact();
  }
}
