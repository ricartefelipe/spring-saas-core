package com.union.solutions.saascore.domain.vo;

import java.util.Objects;
import java.util.UUID;

public record TenantId(UUID value) {

  public TenantId {
    Objects.requireNonNull(value, "TenantId must not be null");
  }

  public static TenantId of(String raw) {
    return new TenantId(UUID.fromString(raw));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
