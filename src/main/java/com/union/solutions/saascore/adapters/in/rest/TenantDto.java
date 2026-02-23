package com.union.solutions.saascore.adapters.in.rest;

import com.union.solutions.saascore.domain.Tenant;
import java.time.Instant;
import java.util.UUID;

public record TenantDto(
    UUID id,
    String name,
    String plan,
    String region,
    String status,
    Instant createdAt,
    Instant updatedAt) {

  public static TenantDto from(Tenant t) {
    return new TenantDto(
        t.getId(),
        t.getName(),
        t.getPlan(),
        t.getRegion(),
        t.getStatus().name(),
        t.getCreatedAt(),
        t.getUpdatedAt());
  }
}
