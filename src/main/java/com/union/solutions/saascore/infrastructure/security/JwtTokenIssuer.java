package com.union.solutions.saascore.infrastructure.security;

import com.union.solutions.saascore.application.port.TokenIssuer;
import io.jsonwebtoken.Jwts;
import java.time.Instant;
import java.util.Date;
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
    public String issue(String sub, String tid, List<String> roles, List<String> perms,
                        String plan, String region) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(sub)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationSeconds)))
                .id(UUID.randomUUID().toString())
                .claims(Map.of(
                        "tid", tid != null ? tid : "",
                        "roles", roles != null ? roles : List.of(),
                        "perms", perms != null ? perms : List.of(),
                        "plan", plan != null ? plan : "",
                        "region", region != null ? region : ""
                ))
                .signWith(secretKey)
                .compact();
    }
}
