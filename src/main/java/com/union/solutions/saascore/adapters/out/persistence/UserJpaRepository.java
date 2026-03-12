package com.union.solutions.saascore.adapters.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmail(String email);

    List<UserEntity> findByTenantId(UUID tenantId);

    boolean existsByEmail(String email);

    long countByTenantId(UUID tenantId);
}
