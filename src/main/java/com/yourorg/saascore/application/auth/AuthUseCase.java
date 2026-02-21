package com.yourorg.saascore.application.auth;

import com.yourorg.saascore.application.port.TokenIssuer;
import com.yourorg.saascore.config.DataSourceRoutingConfig;
import com.yourorg.saascore.config.TenantContext;
import com.yourorg.saascore.domain.Role;
import com.yourorg.saascore.domain.Tenant;
import com.yourorg.saascore.domain.User;
import com.yourorg.saascore.adapters.out.persistence.RoleEntity;
import com.yourorg.saascore.adapters.out.persistence.RoleJpaRepository;
import com.yourorg.saascore.adapters.out.persistence.RolePermissionEntity;
import com.yourorg.saascore.adapters.out.persistence.RolePermissionJpaRepository;
import com.yourorg.saascore.adapters.out.persistence.TenantEntity;
import com.yourorg.saascore.adapters.out.persistence.TenantJpaRepository;
import com.yourorg.saascore.adapters.out.persistence.UserEntity;
import com.yourorg.saascore.adapters.out.persistence.UserJpaRepository;
import com.yourorg.saascore.adapters.out.persistence.UserRoleEntity;
import com.yourorg.saascore.adapters.out.persistence.UserRoleJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthUseCase {

    private static final String ADMIN_EMAIL = "admin@example.com";

    private final UserJpaRepository userRepo;
    private final TenantJpaRepository tenantRepo;
    private final RoleJpaRepository roleRepo;
    private final RolePermissionJpaRepository rolePermRepo;
    private final UserRoleJpaRepository userRoleRepo;
    private final TokenIssuer tokenIssuer;
    private final PasswordEncoder passwordEncoder;

    public AuthUseCase(
            UserJpaRepository userRepo,
            TenantJpaRepository tenantRepo,
            RoleJpaRepository roleRepo,
            RolePermissionJpaRepository rolePermRepo,
            UserRoleJpaRepository userRoleRepo,
            TokenIssuer tokenIssuer,
            PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.tenantRepo = tenantRepo;
        this.roleRepo = roleRepo;
        this.rolePermRepo = rolePermRepo;
        this.userRoleRepo = userRoleRepo;
        this.tokenIssuer = tokenIssuer;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Optional<AuthResult> login(String email, String password) {
        ShardUser shardUser = findUserByEmailAcrossShards(email);
        if (shardUser == null) {
            return Optional.empty();
        }
        UserEntity ue = shardUser.user();
        if (!passwordEncoder.matches(password, ue.getPasswordHash())) {
            return Optional.empty();
        }
        TenantContext.setShardKey(shardUser.shardKey());
        try {
            TenantEntity te = tenantRepo.findById(ue.getTenantId()).orElse(null);
            Tenant tenant = te != null ? te.toDomain() : null;
            User user = ue.toDomain();
            List<String> roles = resolveRoles(ue.getId());
            List<String> perms = resolvePermissions(roles, ue.getTenantId());
            if (ADMIN_EMAIL.equalsIgnoreCase(ue.getEmail())) {
                tenant = null;
            }
            String token = tokenIssuer.issue(user, tenant, roles, perms);
            return Optional.of(new AuthResult(token, user, tenant, roles, perms));
        } finally {
            TenantContext.clear();
        }
    }

    private ShardUser findUserByEmailAcrossShards(String email) {
        TenantContext.setShardKey(DataSourceRoutingConfig.SHARD_A);
        try {
            Optional<UserEntity> u = userRepo.findFirstByEmail(email);
            if (u.isPresent()) return new ShardUser(u.get(), DataSourceRoutingConfig.SHARD_A);
        } finally {
            TenantContext.clear();
        }
        TenantContext.setShardKey(DataSourceRoutingConfig.SHARD_B);
        try {
            Optional<UserEntity> u = userRepo.findFirstByEmail(email);
            if (u.isPresent()) return new ShardUser(u.get(), DataSourceRoutingConfig.SHARD_B);
            return null;
        } finally {
            TenantContext.clear();
        }
    }

    private record ShardUser(UserEntity user, String shardKey) {}

    private List<String> resolveRoles(UUID userId) {
        List<UserRoleEntity> ur = userRoleRepo.findByUserId(userId);
        return ur.stream()
                .map(ure -> roleRepo.findById(ure.getRoleId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(RoleEntity::getName)
                .distinct()
                .toList();
    }

    private List<String> resolvePermissions(List<String> roleNames, UUID tenantId) {
        List<RoleEntity> roles = roleRepo.findByTenantId(tenantId).stream()
                .filter(r -> roleNames.contains(r.getName()))
                .toList();
        return roles.stream()
                .flatMap(r -> rolePermRepo.findByRoleId(r.getId()).stream())
                .map(RolePermissionEntity::getPermissionCode)
                .distinct()
                .toList();
    }

    public record AuthResult(String token, User user, Tenant tenant, List<String> roles, List<String> perms) {}
}
