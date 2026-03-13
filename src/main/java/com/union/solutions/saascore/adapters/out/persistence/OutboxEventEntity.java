package com.union.solutions.saascore.adapters.out.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEventEntity {

  @Id
  @Column(name = "id")
  private UUID id;

  @Column(name = "aggregate_type", nullable = false, length = 64)
  private String aggregateType;

  @Column(name = "aggregate_id", nullable = false)
  private String aggregateId;

  @Column(name = "event_type", nullable = false, length = 128)
  private String eventType;

  @Column(name = "payload", nullable = false, columnDefinition = "text")
  private String payload;

  @Column(name = "status", nullable = false, length = 32)
  private String status;

  @Column(name = "attempts")
  private int attempts;

  @Column(name = "next_attempt_at")
  private Instant nextAttemptAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getAggregateType() {
    return aggregateType;
  }

  public void setAggregateType(String aggregateType) {
    this.aggregateType = aggregateType;
  }

  public String getAggregateId() {
    return aggregateId;
  }

  public void setAggregateId(String aggregateId) {
    this.aggregateId = aggregateId;
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

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public int getAttempts() {
    return attempts;
  }

  public void setAttempts(int attempts) {
    this.attempts = attempts;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof OutboxEventEntity that)) return false;
    return id != null && java.util.Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
