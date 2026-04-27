package com.union.solutions.saascore.adapters.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

  Optional<UserEntity> findByEmail(String email);

  Optional<UserEntity> findByIdAndTenantId(UUID id, UUID tenantId);

  Optional<UserEntity> findByEmailAndTenantId(String email, UUID tenantId);

  List<UserEntity> findByTenantId(UUID tenantId);

  @Transactional
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      """
      UPDATE UserEntity u
         SET u.status = 'DELETED',
             u.updatedAt = :updatedAt
       WHERE u.id = :id
         AND u.tenantId = :tenantId
      """)
  int softDeleteByIdAndTenantId(
      @Param("id") UUID id,
      @Param("tenantId") UUID tenantId,
      @Param("updatedAt") Instant updatedAt);

  boolean existsByEmail(String email);

  long countByTenantId(UUID tenantId);

  List<UserEntity> findByCreatedAtBetween(Instant start, Instant end);

  @Query("SELECT MAX(u.lastLoginAt) FROM UserEntity u WHERE u.tenantId = :tenantId")
  Optional<Instant> findMaxLastLoginAtByTenantId(@Param("tenantId") UUID tenantId);

  @Query("SELECT COUNT(u) FROM UserEntity u WHERE u.tenantId = :tenantId AND u.status = 'ACTIVE'")
  long countActiveByTenantId(@Param("tenantId") UUID tenantId);
}
