package com.union.solutions.saascore.application.port;

import com.union.solutions.saascore.domain.FeatureFlag;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port de persistência de feature flags. A aplicação depende apenas desta interface. */
public interface FeatureFlagRepository {

  FeatureFlag save(FeatureFlag flag);

  /**
   * Cria uma nova flag ou reativa uma flag soft-deleted com o mesmo tenant+name, evitando violação
   * de unique constraint em execuções repetidas (ex.: smoke).
   */
  FeatureFlag createOrResurrect(
      UUID tenantId, String name, boolean enabled, int rolloutPercent, List<String> allowedRoles);

  Optional<FeatureFlag> findByTenantIdAndName(UUID tenantId, String name);

  List<FeatureFlag> findByTenantId(UUID tenantId);

  long countActiveFlags();

  boolean softDelete(UUID tenantId, String name);
}
