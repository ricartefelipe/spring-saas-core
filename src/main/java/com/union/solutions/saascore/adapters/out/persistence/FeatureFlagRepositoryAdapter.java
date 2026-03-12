package com.union.solutions.saascore.adapters.out.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.union.solutions.saascore.application.port.FeatureFlagRepository;
import com.union.solutions.saascore.domain.FeatureFlag;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class FeatureFlagRepositoryAdapter implements FeatureFlagRepository {

  private static final TypeReference<List<String>> LIST_TYPE = new TypeReference<>() {};

  private final FeatureFlagJpaRepository jpa;
  private final ObjectMapper objectMapper;

  public FeatureFlagRepositoryAdapter(FeatureFlagJpaRepository jpa, ObjectMapper objectMapper) {
    this.jpa = jpa;
    this.objectMapper = objectMapper;
  }

  @Override
  public FeatureFlag createOrResurrect(
      UUID tenantId, String name, boolean enabled, int rolloutPercent, List<String> allowedRoles) {
    Optional<FeatureFlagEntity> existing = jpa.findByTenantIdAndNameIncludeDeleted(tenantId, name);
    if (existing.isPresent()) {
      FeatureFlagEntity e = existing.get();
      if (e.isDeleted()) {
        e.setDeleted(false);
        e.setDeletedAt(null);
        e.setEnabled(enabled);
        e.setRolloutPercent(rolloutPercent);
        e.setAllowedRoles(toJson(allowedRoles));
        e.setUpdatedAt(Instant.now());
        return toFlag(jpa.save(e));
      }
      throw new IllegalArgumentException("Flag '" + name + "' already exists for tenant");
    }
    FeatureFlag flag =
        new FeatureFlag(
            UUID.randomUUID(),
            tenantId,
            name,
            enabled,
            rolloutPercent,
            allowedRoles != null ? allowedRoles : List.of(),
            Instant.now(),
            Instant.now());
    return save(flag);
  }

  @Override
  public FeatureFlag save(FeatureFlag flag) {
    FeatureFlagEntity e = toEntity(flag);
    FeatureFlagEntity saved = jpa.save(e);
    return toFlag(saved);
  }

  @Override
  public Optional<FeatureFlag> findByTenantIdAndName(UUID tenantId, String name) {
    return jpa.findByTenantIdAndName(tenantId, name).map(this::toFlag);
  }

  @Override
  public List<FeatureFlag> findByTenantId(UUID tenantId) {
    return jpa.findByTenantId(tenantId).stream().map(this::toFlag).toList();
  }

  @Override
  public long countActiveFlags() {
    return jpa.countActiveFlags();
  }

  @Override
  public boolean softDelete(UUID tenantId, String name) {
    return jpa.findByTenantIdAndName(tenantId, name)
        .map(
            entity -> {
              entity.setDeleted(true);
              entity.setDeletedAt(Instant.now());
              entity.setUpdatedAt(Instant.now());
              jpa.save(entity);
              return true;
            })
        .orElse(false);
  }

  private FeatureFlag toFlag(FeatureFlagEntity e) {
    return new FeatureFlag(
        e.getId(),
        e.getTenantId(),
        e.getName(),
        e.isEnabled(),
        e.getRolloutPercent(),
        parseJson(e.getAllowedRoles()),
        e.getCreatedAt(),
        e.getUpdatedAt());
  }

  private FeatureFlagEntity toEntity(FeatureFlag f) {
    FeatureFlagEntity e = new FeatureFlagEntity();
    e.setId(f.getId());
    e.setTenantId(f.getTenantId());
    e.setName(f.getName());
    e.setEnabled(f.isEnabled());
    e.setRolloutPercent(f.getRolloutPercent());
    e.setAllowedRoles(toJson(f.getAllowedRoles()));
    e.setCreatedAt(f.getCreatedAt());
    e.setUpdatedAt(f.getUpdatedAt());
    e.setDeleted(false);
    e.setDeletedAt(null);
    return e;
  }

  private List<String> parseJson(String json) {
    if (json == null || json.isBlank() || "[]".equals(json)) return List.of();
    try {
      return objectMapper.readValue(json, LIST_TYPE);
    } catch (Exception ex) {
      return List.of();
    }
  }

  private String toJson(List<String> list) {
    try {
      return objectMapper.writeValueAsString(list != null ? list : List.of());
    } catch (Exception ex) {
      return "[]";
    }
  }
}
