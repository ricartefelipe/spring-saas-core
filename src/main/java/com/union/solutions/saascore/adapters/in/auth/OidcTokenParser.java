package com.union.solutions.saascore.adapters.in.auth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.auth.mode", havingValue = "oidc")
public class OidcTokenParser implements TokenParser {

  private static final Logger log = LoggerFactory.getLogger(OidcTokenParser.class);

  private final JwtDecoder jwtDecoder;
  private final String clientId;

  public OidcTokenParser(
      JwtDecoder jwtDecoder,
      @Value("${app.auth.oidc.client-id:spring-saas-core}") String clientId) {
    this.jwtDecoder = jwtDecoder;
    this.clientId = clientId;
  }

  @Override
  public Optional<TokenClaims> parse(String token) {
    try {
      Jwt jwt = jwtDecoder.decode(token);
      return Optional.of(toClaims(jwt));
    } catch (Exception e) {
      log.debug("OIDC token parse failed: {}", e.getMessage());
      return Optional.empty();
    }
  }

  private TokenClaims toClaims(Jwt jwt) {
    String sub = jwt.getSubject();
    String tid = getStringClaim(jwt, "tid");
    String plan = getStringClaim(jwt, "plan");
    String region = getStringClaim(jwt, "region");

    List<String> roles = new ArrayList<>();
    roles.addAll(extractRealmRoles(jwt));
    roles.addAll(extractClientRoles(jwt));

    List<String> perms = getStringListClaim(jwt, "perms");
    if (perms.isEmpty()) {
      perms = getStringListClaim(jwt, "permissions");
    }

    return new TokenClaims(
        sub, tid, roles, perms, plan != null ? plan : "", region != null ? region : "");
  }

  private List<String> extractRealmRoles(Jwt jwt) {
    Object realmAccess = jwt.getClaims().get("realm_access");
    if (realmAccess instanceof Map<?, ?> map) {
      Object rolesObj = map.get("roles");
      if (rolesObj instanceof List<?> list) {
        return list.stream().map(Object::toString).toList();
      }
    }
    return getStringListClaim(jwt, "roles");
  }

  private List<String> extractClientRoles(Jwt jwt) {
    Object resourceAccess = jwt.getClaims().get("resource_access");
    if (resourceAccess instanceof Map<?, ?> resources) {
      Object clientResource = resources.get(clientId);
      if (clientResource instanceof Map<?, ?> clientMap) {
        Object rolesObj = clientMap.get("roles");
        if (rolesObj instanceof List<?> list) {
          return list.stream().map(Object::toString).toList();
        }
      }
    }
    return Collections.emptyList();
  }

  private String getStringClaim(Jwt jwt, String name) {
    Object v = jwt.getClaims().get(name);
    return v != null ? v.toString() : null;
  }

  private List<String> getStringListClaim(Jwt jwt, String name) {
    Object v = jwt.getClaims().get(name);
    if (v instanceof List<?> list) {
      return list.stream().map(Object::toString).toList();
    }
    return Collections.emptyList();
  }
}
