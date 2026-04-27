package com.union.solutions.saascore.adapters.out.persistence;

import com.union.solutions.saascore.application.port.UserRepository;
import com.union.solutions.saascore.domain.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class UserRepositoryAdapter implements UserRepository {

  private final UserJpaRepository jpa;

  public UserRepositoryAdapter(UserJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public User save(User user) {
    return jpa.save(UserEntity.from(user)).toDomain();
  }

  @Override
  public Optional<User> findById(UUID id) {
    return jpa.findById(id).map(UserEntity::toDomain);
  }

  @Override
  public Optional<User> findByEmail(String email) {
    return jpa.findByEmail(email).map(UserEntity::toDomain);
  }

  @Override
  public List<User> findByTenantId(UUID tenantId) {
    return jpa.findByTenantId(tenantId).stream().map(UserEntity::toDomain).toList();
  }

  @Override
  public Optional<User> findByIdAndTenantId(UUID id, UUID tenantId) {
    return jpa.findByIdAndTenantId(id, tenantId).map(UserEntity::toDomain);
  }

  @Override
  public Optional<User> findByEmailAndTenantId(String email, UUID tenantId) {
    return jpa.findByEmailAndTenantId(email, tenantId).map(UserEntity::toDomain);
  }

  @Override
  public boolean softDeleteByIdAndTenantId(UUID id, UUID tenantId, Instant updatedAt) {
    return jpa.softDeleteByIdAndTenantId(id, tenantId, updatedAt) > 0;
  }

  @Override
  public boolean existsByEmail(String email) {
    return jpa.existsByEmail(email);
  }

  @Override
  public long countByTenantId(UUID tenantId) {
    return jpa.countByTenantId(tenantId);
  }

  @Override
  public List<User> findByCreatedAtBetween(Instant start, Instant end) {
    return jpa.findByCreatedAtBetween(start, end).stream().map(UserEntity::toDomain).toList();
  }

  @Override
  public Optional<Instant> findMaxLastLoginAtByTenantId(UUID tenantId) {
    return jpa.findMaxLastLoginAtByTenantId(tenantId);
  }

  @Override
  public long countActiveByTenantId(UUID tenantId) {
    return jpa.countActiveByTenantId(tenantId);
  }
}
