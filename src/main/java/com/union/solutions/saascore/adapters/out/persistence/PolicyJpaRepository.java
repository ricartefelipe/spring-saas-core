package com.union.solutions.saascore.adapters.out.persistence;

import com.union.solutions.saascore.domain.Policy;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PolicyJpaRepository extends JpaRepository<PolicyEntity, UUID> {

  @Query(
      """
        SELECT p FROM PolicyEntity p
        WHERE p.deleted = false
        AND (:permissionCode IS NULL OR p.permissionCode = :permissionCode)
        AND (:effect IS NULL OR p.effect = :effect)
        AND (:enabled IS NULL OR p.enabled = :enabled)
    """)
  Page<PolicyEntity> search(
      @Param("permissionCode") String permissionCode,
      @Param("effect") Policy.Effect effect,
      @Param("enabled") Boolean enabled,
      Pageable pageable);

  @Query("SELECT p FROM PolicyEntity p WHERE p.id = :id AND p.deleted = false")
  Optional<PolicyEntity> findActiveById(@Param("id") UUID id);

  @Query(
      "SELECT p FROM PolicyEntity p WHERE p.permissionCode = :code AND p.enabled = true AND p.deleted = false")
  List<PolicyEntity> findByPermissionCodeAndEnabledTrue(@Param("code") String permissionCode);

  @Query("SELECT p FROM PolicyEntity p WHERE p.enabled = true AND p.deleted = false")
  List<PolicyEntity> findByEnabledTrue();

  @Query("SELECT COUNT(p) FROM PolicyEntity p WHERE p.deleted = false")
  long countActive();

  @Query(
      """
        SELECT p FROM PolicyEntity p
        WHERE p.deleted = false
        AND p.createdAt > :cursor
        ORDER BY p.createdAt ASC
    """)
  List<PolicyEntity> findNextPage(@Param("cursor") java.time.Instant cursor, Pageable pageable);

  @Query("SELECT p.effect, COUNT(p) FROM PolicyEntity p WHERE p.deleted = false GROUP BY p.effect")
  List<Object[]> countActiveGroupByEffect();

  @Query(
      "SELECT p.permissionCode, COUNT(p) FROM PolicyEntity p WHERE p.deleted = false"
          + " GROUP BY p.permissionCode ORDER BY COUNT(p) DESC")
  List<Object[]> countActiveGroupByPermissionCode();
}
