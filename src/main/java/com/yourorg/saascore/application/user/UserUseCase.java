package com.yourorg.saascore.application.user;

import com.yourorg.saascore.config.TenantContext;
import com.yourorg.saascore.domain.User;
import com.yourorg.saascore.adapters.out.persistence.*;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserUseCase {

    private final UserJpaRepository userRepo;
    private final RoleJpaRepository roleRepo;
    private final UserRoleJpaRepository userRoleRepo;
    private final IdempotencyKeyJpaRepository idempotencyRepo;
    private final PasswordEncoder passwordEncoder;

    public UserUseCase(
            UserJpaRepository userRepo,
            RoleJpaRepository roleRepo,
            UserRoleJpaRepository userRoleRepo,
            IdempotencyKeyJpaRepository idempotencyRepo,
            PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.userRoleRepo = userRoleRepo;
        this.idempotencyRepo = idempotencyRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public IdempotentResult createUser(UUID tenantId, String email, String password, String idempotencyKey) {
        UUID tenant = tenantId != null ? tenantId : TenantContext.getTenantId().orElse(null);
        if (tenant == null) {
            throw new IllegalArgumentException("tenant_id required");
        }
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<IdempotencyKeyEntity> existing = idempotencyRepo.findByTenantIdAndIdempotencyKey(tenant, idempotencyKey);
            if (existing.isPresent()) {
                IdempotencyKeyEntity e = existing.get();
                return new IdempotentResult(e.getResponseStatus(), e.getResponseBody(), true);
            }
        }
        if (userRepo.existsByTenantIdAndEmail(tenant, email)) {
            Optional<UserEntity> u = userRepo.findByTenantIdAndEmail(tenant, email);
            if (u.isPresent()) {
                User created = u.get().toDomain();
                int status = 200;
                String body = "{\"id\":\"" + created.getId() + "\",\"email\":\"" + created.getEmail() + "\"}";
                if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                    saveIdempotency(tenant, idempotencyKey, status, body);
                }
                return new IdempotentResult(status, body, true);
            }
        }
        UUID id = UUID.randomUUID();
        User user = new User(id, tenant, email, passwordEncoder.encode(password), User.UserStatus.ACTIVE, Instant.now());
        UserEntity e = UserEntity.from(user);
        userRepo.save(e);
        int status = 201;
        String body = "{\"id\":\"" + id + "\",\"email\":\"" + email + "\"}";
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            saveIdempotency(tenant, idempotencyKey, status, body);
        }
        return new IdempotentResult(status, body, false);
    }

    private void saveIdempotency(UUID tenantId, String key, int status, String body) {
        IdempotencyKeyEntity ik = new IdempotencyKeyEntity();
        ik.setTenantId(tenantId);
        ik.setIdempotencyKey(key);
        ik.setResponseStatus(status);
        ik.setResponseBody(body);
        ik.setExpires_at(Instant.now().plusSeconds(86400));
        idempotencyRepo.save(ik);
    }

    @Transactional
    public boolean assignRole(UUID userId, String roleName) {
        UUID tenantId = TenantContext.getTenantId().orElse(null);
        if (tenantId == null) return false;
        Optional<UserEntity> user = userRepo.findByIdAndTenantId(userId, tenantId);
        if (user.isEmpty()) return false;
        Optional<RoleEntity> role = roleRepo.findByTenantIdAndName(tenantId, roleName);
        if (role.isEmpty()) return false;
        if (userRoleRepo.existsByUserIdAndRoleId(userId, role.get().getId())) return true;
        UserRoleEntity ur = new UserRoleEntity();
        ur.setUserId(userId);
        ur.setRoleId(role.get().getId());
        userRoleRepo.save(ur);
        return true;
    }

    public record IdempotentResult(int status, String body, boolean replay) {}
}
