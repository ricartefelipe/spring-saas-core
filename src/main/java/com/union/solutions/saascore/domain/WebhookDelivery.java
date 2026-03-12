package com.union.solutions.saascore.domain;

import java.time.Instant;
import java.util.UUID;

public class WebhookDelivery {

  public enum Status {
    PENDING,
    DELIVERED,
    FAILED
  }

  private UUID id;
  private UUID endpointId;
  private UUID tenantId;
  private String eventType;
  private String payload;
  private Status status;
  private int attempts;
  private Integer responseCode;
  private Instant nextAttemptAt;
  private Instant createdAt;
  private Instant updatedAt;

  public WebhookDelivery() {}

  public WebhookDelivery(
      UUID id,
      UUID endpointId,
      UUID tenantId,
      String eventType,
      String payload,
      Status status,
      int attempts,
      Integer responseCode,
      Instant nextAttemptAt,
      Instant createdAt,
      Instant updatedAt) {
    this.id = id;
    this.endpointId = endpointId;
    this.tenantId = tenantId;
    this.eventType = eventType;
    this.payload = payload;
    this.status = status;
    this.attempts = attempts;
    this.responseCode = responseCode;
    this.nextAttemptAt = nextAttemptAt;
    this.createdAt = createdAt != null ? createdAt : Instant.now();
    this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getEndpointId() {
    return endpointId;
  }

  public void setEndpointId(UUID endpointId) {
    this.endpointId = endpointId;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public void setTenantId(UUID tenantId) {
    this.tenantId = tenantId;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public String getPayload() {
    return payload;
  }

  public void setPayload(String payload) {
    this.payload = payload;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public int getAttempts() {
    return attempts;
  }

  public void setAttempts(int attempts) {
    this.attempts = attempts;
  }

  public Integer getResponseCode() {
    return responseCode;
  }

  public void setResponseCode(Integer responseCode) {
    this.responseCode = responseCode;
  }

  public Instant getNextAttemptAt() {
    return nextAttemptAt;
  }

  public void setNextAttemptAt(Instant nextAttemptAt) {
    this.nextAttemptAt = nextAttemptAt;
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
