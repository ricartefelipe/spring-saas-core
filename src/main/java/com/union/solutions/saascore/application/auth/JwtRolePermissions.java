package com.union.solutions.saascore.application.auth;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Permissões embutidas no JWT por role, alinhadas ao {@code UserUseCase} e às políticas ABAC em BD.
 */
public final class JwtRolePermissions {

  private static final Map<String, List<String>> BY_ROLE;

  static {
    Map<String, List<String>> m = new HashMap<>();
    m.put(
        "admin",
        List.of(
            "tenants:read",
            "tenants:write",
            "policies:read",
            "policies:write",
            "flags:read",
            "flags:write",
            "audit:read",
            "analytics:read",
            "admin:write",
            "users:read",
            "users:write",
            "orders:read",
            "orders:write",
            "inventory:read",
            "inventory:write",
            "payments:read",
            "payments:write",
            "ledger:read",
            "products:read",
            "products:write",
            "billing:write",
            "profile:read"));
    m.put(
        "ops",
        List.of(
            "orders:read",
            "orders:write",
            "inventory:read",
            "inventory:write",
            "products:read",
            "products:write",
            "payments:read",
            "payments:write",
            "ledger:read",
            "profile:read"));
    m.put(
        "viewer",
        List.of(
            "orders:read",
            "inventory:read",
            "payments:read",
            "ledger:read",
            "products:read",
            "profile:read"));
    m.put("member", List.of("products:read", "orders:read", "profile:read"));
    BY_ROLE = Map.copyOf(m);
  }

  private JwtRolePermissions() {}

  public static List<String> forRoles(List<String> roles) {
    if (roles == null || roles.isEmpty()) {
      return List.of();
    }
    return roles.stream()
        .flatMap(r -> BY_ROLE.getOrDefault(r, List.of()).stream())
        .distinct()
        .toList();
  }
}
