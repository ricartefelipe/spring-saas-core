package com.yourorg.saascore.config;

import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {

    @Bean
    public SecretKey jwtSecretKey(@Value("${app.jwt.secret}") String secret) {
        return javax.crypto.spec.SecretKeySpec.of(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
    }
}
