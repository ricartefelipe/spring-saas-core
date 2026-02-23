package com.union.solutions.saascore.adapters.in.auth;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.auth.mode", havingValue = "oidc")
public class OidcTokenParser implements TokenParser {

  private final JwtDecoder jwtDecoder;

  public OidcTokenParser(JwtDecoder jwtDecoder) {
    this.jwtDecoder = jwtDecoder;
  }

  @Override
  public Optional<TokenClaims> parse(String token) {
    try {
      Jwt jwt = jwtDecoder.decode(token);
      return Optional.of(toClaims(jwt));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  private TokenClaims toClaims(Jwt jwt) {
    String sub = jwt.getSubject();
    String tid = getStringClaim(jwt, "tid");
    String plan = getStringClaim(jwt, "plan");
    String region = getStringClaim(jwt, "region");
    List<String> roles = getStringListClaim(jwt, "roles");
    List<String> perms = getStringListClaim(jwt, "perms");
    return new TokenClaims(
        sub, tid, roles, perms, plan != null ? plan : "", region != null ? region : "");
  }

  private String getStringClaim(Jwt jwt, String name) {
    Object v = jwt.getClaims().get(name);
    return v != null ? v.toString() : null;
  }

  @SuppressWarnings("unchecked")
  private List<String> getStringListClaim(Jwt jwt, String name) {
    Object v = jwt.getClaims().get(name);
    if (v instanceof List<?> list) {
      return list.stream().map(Object::toString).toList();
    }
    return Collections.emptyList();
  }
}
