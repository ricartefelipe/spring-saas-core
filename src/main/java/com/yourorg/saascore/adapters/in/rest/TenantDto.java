package com.yourorg.saascore.adapters.in.rest;

import com.yourorg.saascore.domain.Tenant;
import java.time.Instant;
import java.util.UUID;

public record TenantDto(
        UUID id,
        String name,
        String status,
        String plan,
        String primaryRegion,
        String shardKey,
        Instant createdAt) {

    public static TenantDto from(Tenant t) {
        return new TenantDto(
                t.getId(),
                t.getName(),
                t.getStatus().name(),
                t.getPlan().name(),
                t.getPrimaryRegion(),
                t.getShardKey(),
                t.getCreatedAt());
    }
}
