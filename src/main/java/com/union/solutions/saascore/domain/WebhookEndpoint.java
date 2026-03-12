package com.union.solutions.saascore.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class WebhookEndpoint {

  private UUID id;
  private UUID tenantId;
  private String url;
  private String secret;
  private List<String> events;
  private boolean active;
  private Instant createdAt;
  private Instant updatedAt;

  public WebhookEndpoint() {}

  public WebhookEndpoint(
      UUID id,
      UUID tenantId,
      String url,
      String secret,
      List<String> events,
      boolean active,
      Instant createdAt,
      Instant updatedAt) {
    this.id = id;
    this.tenantId = tenantId;
    this.url = url;
    this.secret = secret;
    this.events = events != null ? events : List.of();
    this.active = active;
    this.createdAt = createdAt != null ? createdAt : Instant.now();
    this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
  }

  public boolean subscribesTo(String eventType) {
    if (events == null || events.isEmpty()) return false;
    return events.contains("*") || events.contains(eventType);
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public void setTenantId(UUID tenantId) {
    this.tenantId = tenantId;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }

  public List<String> getEvents() {
    return events;
  }

  public void setEvents(List<String> events) {
    this.events = events;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
