package com.union.solutions.saascore.application.port;

import com.union.solutions.saascore.domain.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

  User save(User user);

  Optional<User> findById(UUID id);

  Optional<User> findByIdAndTenantId(UUID id, UUID tenantId);

  Optional<User> findByEmail(String email);

  Optional<User> findByEmailAndTenantId(String email, UUID tenantId);

  List<User> findByTenantId(UUID tenantId);

  boolean softDeleteByIdAndTenantId(UUID id, UUID tenantId, Instant updatedAt);

  boolean existsByEmail(String email);

  long countByTenantId(UUID tenantId);

  /** Users with created_at in [start, end] (inclusive). Used for post-signup email scheduling. */
  List<User> findByCreatedAtBetween(Instant start, Instant end);

  /** Latest lastLoginAt among users of the tenant, or empty if none. */
  Optional<Instant> findMaxLastLoginAtByTenantId(UUID tenantId);

  /** Count of users with status ACTIVE in the tenant. */
  long countActiveByTenantId(UUID tenantId);
}
