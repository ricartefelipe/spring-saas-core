package com.union.solutions.saascore.application.auth;

/**
 * Alinha slugs de plano e região do Core com os valores reconhecidos por ABAC em node-b2b-orders e
 * py-payments-ledger (ex.: signup com "professional", default legado "us-east-1").
 */
public final class JwtTenantClaimsNormalizer {

  private JwtTenantClaimsNormalizer() {}

  public static String plan(String raw) {
    if (raw == null || raw.isBlank()) {
      return "starter";
    }
    String p = raw.trim().toLowerCase();
    if ("professional".equals(p)) {
      return "pro";
    }
    return raw.trim();
  }

  public static String region(String raw) {
    if (raw == null || raw.isBlank()) {
      return "region-a";
    }
    String r = raw.trim().toLowerCase();
    if ("us-east-1".equals(r)) {
      return "region-a";
    }
    return raw.trim();
  }
}
