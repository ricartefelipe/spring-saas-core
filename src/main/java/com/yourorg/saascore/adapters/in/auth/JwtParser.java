package com.yourorg.saascore.adapters.in.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtParser {

    private final SecretKey key;

    public JwtParser(javax.crypto.SecretKey jwtSecretKey) {
        this.key = jwtSecretKey;
    }

    public Optional<Claims> parse(String token) {
        try {
            Claims c =
                    Jwts.parser()
                            .verifyWith(key)
                            .build()
                            .parseSignedClaims(token)
                            .getPayload();
            return Optional.of(c);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
