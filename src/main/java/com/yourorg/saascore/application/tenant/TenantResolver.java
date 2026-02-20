package com.yourorg.saascore.application.tenant;

import com.yourorg.saascore.config.DataSourceRoutingConfig;
import com.yourorg.saascore.config.TenantContext;
import com.yourorg.saascore.domain.Tenant;
import com.yourorg.saascore.adapters.out.persistence.TenantEntity;
import com.yourorg.saascore.adapters.out.persistence.TenantJpaRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TenantResolver {

    private final TenantJpaRepository tenantRepo;

    public TenantResolver(TenantJpaRepository tenantRepo) {
        this.tenantRepo = tenantRepo;
    }

    public Optional<Tenant> resolveAndSetContext(UUID tenantId) {
        if (tenantId == null) return Optional.empty();
        TenantContext.setShardKey(DataSourceRoutingConfig.SHARD_A);
        Optional<TenantEntity> tA = tenantRepo.findById(tenantId);
        if (tA.isPresent()) {
            Tenant tenant = tA.get().toDomain();
            TenantContext.setTenantId(tenantId);
            TenantContext.setShardKey(tenant.getShardKey());
            TenantContext.setPrimaryRegion(tenant.getPrimaryRegion());
            return Optional.of(tenant);
        }
        TenantContext.setShardKey(DataSourceRoutingConfig.SHARD_B);
        Optional<TenantEntity> tB = tenantRepo.findById(tenantId);
        if (tB.isPresent()) {
            Tenant tenant = tB.get().toDomain();
            TenantContext.setTenantId(tenantId);
            TenantContext.setShardKey(tenant.getShardKey());
            TenantContext.setPrimaryRegion(tenant.getPrimaryRegion());
            return Optional.of(tenant);
        }
        return Optional.empty();
    }
}
