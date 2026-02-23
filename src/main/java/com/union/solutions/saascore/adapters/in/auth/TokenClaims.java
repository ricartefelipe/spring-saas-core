package com.union.solutions.saascore.adapters.in.auth;

import java.util.List;

/** DTO para claims JWT extraídas por HS256 ou OIDC. */
public record TokenClaims(
    String sub, String tid, List<String> roles, List<String> perms, String plan, String region) {

  public static TokenClaims empty() {
    return new TokenClaims(null, null, List.of(), List.of(), "", "");
  }
}
