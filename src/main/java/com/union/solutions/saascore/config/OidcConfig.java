package com.union.solutions.saascore.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
@ConditionalOnProperty(name = "app.auth.mode", havingValue = "oidc")
public class OidcConfig {

  @Bean
  public JwtDecoder jwtDecoder(
      @Value("${app.auth.oidc.jwk-set-uri:}") String jwkSetUri,
      @Value("${app.auth.oidc.issuer-uri:}") String issuerUri,
      @Value("${app.auth.oidc.audience:}") String audience) {
    NimbusJwtDecoder decoder;
    if (jwkSetUri != null && !jwkSetUri.isBlank()) {
      decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    } else if (issuerUri != null && !issuerUri.isBlank()) {
      decoder = NimbusJwtDecoder.withIssuerLocation(issuerUri).build();
    } else {
      throw new IllegalStateException(
          "OIDC mode requires app.auth.oidc.jwk-set-uri or app.auth.oidc.issuer-uri. "
              + "Example: OIDC_ISSUER_URI=https://keycloak.example.com/realms/saas");
    }

    DelegatingOAuth2TokenValidator<Jwt> validators;
    if (issuerUri != null && !issuerUri.isBlank()) {
      validators =
          new DelegatingOAuth2TokenValidator<>(
              new JwtTimestampValidator(), new JwtIssuerValidator(issuerUri));
    } else {
      validators = new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator());
    }
    decoder.setJwtValidator(validators);

    return decoder;
  }
}
