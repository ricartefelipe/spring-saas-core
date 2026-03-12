package com.union.solutions.saascore.adapters.out.persistence;

import com.union.solutions.saascore.application.port.UserRepository;
import com.union.solutions.saascore.domain.User;
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
    public boolean existsByEmail(String email) {
        return jpa.existsByEmail(email);
    }

    @Override
    public long countByTenantId(UUID tenantId) {
        return jpa.countByTenantId(tenantId);
    }
}
