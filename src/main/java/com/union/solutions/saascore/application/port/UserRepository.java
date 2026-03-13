package com.union.solutions.saascore.application.port;

import com.union.solutions.saascore.domain.User;
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

  boolean existsByEmail(String email);

  long countByTenantId(UUID tenantId);
}
