package com.union.solutions.saascore.application.tenant;

import com.union.solutions.saascore.config.TenantContext;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;

/** Regra única de isolamento: a plataforma alcança todos os locatários; os demais, só o próprio. */
public final class TenantAccess {

  private TenantAccess() {}

  public static void requirePlatform() {
    if (!TenantContext.isPlatformAdmin()) {
      throw new AccessDeniedException("Restricted to platform administrators");
    }
  }

  public static void requireTenant(UUID tenantId) {
    if (TenantContext.isPlatformAdmin()) {
      return;
    }
    if (!TenantContext.getTenantId().filter(tenantId::equals).isPresent()) {
      throw new AccessDeniedException("Tenant mismatch");
    }
  }

  /** Locatário a consultar: a plataforma escolhe; os demais ficam presos ao próprio. */
  public static UUID scope(UUID requestedTenantId) {
    if (TenantContext.isPlatformAdmin()) {
      return requestedTenantId;
    }
    UUID own =
        TenantContext.getTenantId()
            .orElseThrow(() -> new AccessDeniedException("Tenant context required"));
    if (requestedTenantId != null && !own.equals(requestedTenantId)) {
      throw new AccessDeniedException("Tenant mismatch");
    }
    return own;
  }
}
