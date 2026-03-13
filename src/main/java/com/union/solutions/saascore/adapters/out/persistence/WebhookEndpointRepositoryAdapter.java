package com.union.solutions.saascore.adapters.out.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.union.solutions.saascore.application.port.WebhookEndpointRepository;
import com.union.solutions.saascore.domain.WebhookEndpoint;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class WebhookEndpointRepositoryAdapter implements WebhookEndpointRepository {

  private static final TypeReference<List<String>> LIST_TYPE = new TypeReference<>() {};

  private final WebhookEndpointJpaRepository jpa;
  private final ObjectMapper objectMapper;

  public WebhookEndpointRepositoryAdapter(
      WebhookEndpointJpaRepository jpa, ObjectMapper objectMapper) {
    this.jpa = jpa;
    this.objectMapper = objectMapper;
  }

  @Override
  public WebhookEndpoint save(WebhookEndpoint endpoint) {
    WebhookEndpointEntity e = toEntity(endpoint);
    WebhookEndpointEntity saved = jpa.save(e);
    return toDomain(saved);
  }

  @Override
  public Optional<WebhookEndpoint> findByIdAndTenantId(UUID id, UUID tenantId) {
    return jpa.findById(id)
        .filter(entity -> tenantId.equals(entity.getTenantId()))
        .map(this::toDomain);
  }

  @Override
  public List<WebhookEndpoint> findByTenantIdAndActiveTrue(UUID tenantId) {
    return jpa.findByTenantIdAndActiveTrue(tenantId).stream().map(this::toDomain).toList();
  }

  @Override
  public boolean deleteByIdAndTenantId(UUID id, UUID tenantId) {
    return jpa.findById(id)
        .filter(entity -> tenantId.equals(entity.getTenantId()))
        .map(
            entity -> {
              jpa.delete(entity);
              return true;
            })
        .orElse(false);
  }

  private WebhookEndpoint toDomain(WebhookEndpointEntity e) {
    return new WebhookEndpoint(
        e.getId(),
        e.getTenantId(),
        e.getUrl(),
        e.getSecret(),
        parseJson(e.getEvents()),
        e.isActive(),
        e.getCreatedAt(),
        e.getUpdatedAt());
  }

  private WebhookEndpointEntity toEntity(WebhookEndpoint w) {
    WebhookEndpointEntity e = new WebhookEndpointEntity();
    e.setId(w.getId());
    e.setTenantId(w.getTenantId());
    e.setUrl(w.getUrl());
    e.setSecret(w.getSecret());
    e.setEvents(toJson(w.getEvents()));
    e.setActive(w.isActive());
    Instant now = Instant.now();
    e.setCreatedAt(w.getCreatedAt() != null ? w.getCreatedAt() : now);
    e.setUpdatedAt(w.getUpdatedAt() != null ? w.getUpdatedAt() : now);
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
