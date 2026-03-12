package com.union.solutions.saascore.adapters.out.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.union.solutions.saascore.application.port.PolicyRepository;
import com.union.solutions.saascore.domain.Policy;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class PolicyRepositoryAdapter implements PolicyRepository {

  private static final TypeReference<List<String>> LIST_TYPE = new TypeReference<>() {};

  private final PolicyJpaRepository jpa;
  private final ObjectMapper objectMapper;

  public PolicyRepositoryAdapter(PolicyJpaRepository jpa, ObjectMapper objectMapper) {
    this.jpa = jpa;
    this.objectMapper = objectMapper;
  }

  @Override
  public Policy save(Policy policy) {
    PolicyEntity e = toEntity(policy);
    PolicyEntity saved = jpa.save(e);
    return toPolicy(saved);
  }

  @Override
  public Optional<Policy> findActiveById(UUID id) {
    return jpa.findActiveById(id).map(this::toPolicy);
  }

  @Override
  public Page<Policy> search(
      String permissionCode, Policy.Effect effect, Boolean enabled, Pageable pageable) {
    return jpa.search(permissionCode, effect, enabled, pageable).map(this::toPolicy);
  }

  @Override
  public List<Policy> findByEnabledTrue() {
    return jpa.findByEnabledTrue().stream().map(this::toPolicy).toList();
  }

  @Override
  public List<Policy> findByPermissionCodeAndEnabledTrue(String permissionCode) {
    return jpa.findByPermissionCodeAndEnabledTrue(permissionCode).stream()
        .map(this::toPolicy)
        .toList();
  }

  @Override
  public long countActive() {
    return jpa.countActive();
  }

  @Override
  public boolean softDelete(UUID id) {
    return jpa.findActiveById(id)
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

  private Policy toPolicy(PolicyEntity e) {
    return new Policy(
        e.getId(),
        e.getPermissionCode(),
        e.getEffect(),
        parseJson(e.getAllowedPlans()),
        parseJson(e.getAllowedRegions()),
        e.isEnabled(),
        e.getNotes(),
        e.getCreatedAt(),
        e.getUpdatedAt());
  }

  private PolicyEntity toEntity(Policy p) {
    PolicyEntity e = new PolicyEntity();
    e.setId(p.getId());
    e.setPermissionCode(p.getPermissionCode());
    e.setEffect(p.getEffect());
    e.setAllowedPlans(toJson(p.getAllowedPlans()));
    e.setAllowedRegions(toJson(p.getAllowedRegions()));
    e.setEnabled(p.isEnabled());
    e.setNotes(p.getNotes());
    e.setCreatedAt(p.getCreatedAt());
    e.setUpdatedAt(p.getUpdatedAt());
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
