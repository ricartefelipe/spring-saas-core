package com.union.solutions.saascore.adapters.out.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.union.solutions.saascore.application.port.OutboxPublisherPort;
import com.union.solutions.saascore.application.port.WebhookEnqueuerPort;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class JpaOutboxPublisher implements OutboxPublisherPort {

  private final OutboxEventJpaRepository outboxRepo;
  private final ObjectMapper objectMapper;
  private final WebhookEnqueuerPort webhookEnqueuer;

  public JpaOutboxPublisher(
      OutboxEventJpaRepository outboxRepo,
      ObjectMapper objectMapper,
      WebhookEnqueuerPort webhookEnqueuer) {
    this.outboxRepo = outboxRepo;
    this.objectMapper = objectMapper;
    this.webhookEnqueuer = webhookEnqueuer;
  }

  @Override
  public void publish(
      String aggregateType, String aggregateId, String eventType, Map<String, String> payload) {
    OutboxEventEntity outbox = new OutboxEventEntity();
    outbox.setId(UUID.randomUUID());
    outbox.setAggregateType(aggregateType);
    outbox.setAggregateId(aggregateId);
    outbox.setEventType(eventType);
    String payloadJson = writeJson(payload);
    outbox.setPayload(payloadJson);
    outbox.setStatus("PENDING");
    outbox.setAttempts(0);
    Instant now = Instant.now();
    outbox.setCreatedAt(now);
    outbox.setUpdatedAt(now);
    outboxRepo.save(outbox);

    UUID tenantId = resolveTenantId(aggregateType, aggregateId, payload);
    webhookEnqueuer.enqueue(tenantId, eventType, payloadJson);
  }

  private UUID resolveTenantId(
      String aggregateType, String aggregateId, Map<String, String> payload) {
    String tenantIdStr = payload != null ? payload.get("tenantId") : null;
    if (tenantIdStr != null && !tenantIdStr.isBlank()) {
      try {
        return UUID.fromString(tenantIdStr);
      } catch (IllegalArgumentException ignored) {
        // fall through
      }
    }
    if ("TENANT".equals(aggregateType) && aggregateId != null && !aggregateId.isBlank()) {
      try {
        return UUID.fromString(aggregateId);
      } catch (IllegalArgumentException ignored) {
        // fall through
      }
    }
    if ("ONBOARDING".equals(aggregateType) && aggregateId != null && !aggregateId.isBlank()) {
      try {
        return UUID.fromString(aggregateId);
      } catch (IllegalArgumentException ignored) {
        // fall through
      }
    }
    return null;
  }

  private String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      return "{}";
    }
  }
}
