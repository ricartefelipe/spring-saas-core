package com.union.solutions.saascore.adapters.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeatureFlagJpaRepository extends JpaRepository<FeatureFlagEntity, UUID> {

  @Query("SELECT f FROM FeatureFlagEntity f WHERE f.tenantId = :tenantId AND f.deleted = false")
  List<FeatureFlagEntity> findByTenantId(@Param("tenantId") UUID tenantId);

  @Query(
      "SELECT f FROM FeatureFlagEntity f WHERE f.tenantId = :tenantId AND f.name = :name AND f.deleted = false")
  Optional<FeatureFlagEntity> findByTenantIdAndName(
      @Param("tenantId") UUID tenantId, @Param("name") String name);

  @Query("SELECT f FROM FeatureFlagEntity f WHERE f.tenantId = :tenantId AND f.name = :name")
  Optional<FeatureFlagEntity> findByTenantIdAndNameIncludeDeleted(
      @Param("tenantId") UUID tenantId, @Param("name") String name);

  @Query("SELECT COUNT(f) FROM FeatureFlagEntity f WHERE f.enabled = true AND f.deleted = false")
  long countActiveFlags();

  @Query(
      "SELECT COUNT(f) FROM FeatureFlagEntity f WHERE f.tenantId = :tenantId AND f.enabled = true AND f.deleted = false")
  long countActiveFlagsByTenant(@Param("tenantId") UUID tenantId);

  @Query("SELECT COUNT(f) FROM FeatureFlagEntity f WHERE f.deleted = false")
  long countTotalNonDeleted();

  @Query("SELECT COUNT(f) FROM FeatureFlagEntity f WHERE f.enabled = false AND f.deleted = false")
  long countDisabledFlags();
}
