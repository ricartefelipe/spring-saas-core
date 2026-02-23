package com.union.solutions.saascore.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
@ConditionalOnProperty(name = "app.auth.mode", havingValue = "oidc")
public class OidcConfig {

  @Bean
  public JwtDecoder jwtDecoder(
      @Value("${app.auth.oidc.jwk-set-uri:}") String jwkSetUri,
      @Value("${app.auth.oidc.issuer-uri:}") String issuerUri) {
    if (jwkSetUri != null && !jwkSetUri.isBlank()) {
      return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }
    if (issuerUri != null && !issuerUri.isBlank()) {
      return NimbusJwtDecoder.withIssuerLocation(issuerUri).build();
    }
    throw new IllegalStateException(
        "OIDC mode requires app.auth.oidc.jwk-set-uri or app.auth.oidc.issuer-uri to be set. "
            + "Example: OIDC_ISSUER_URI=https://keycloak.example.com/realms/saas");
  }
}
