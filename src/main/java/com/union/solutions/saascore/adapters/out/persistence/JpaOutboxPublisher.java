package com.union.solutions.saascore.adapters.out.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.union.solutions.saascore.application.port.OutboxPublisherPort;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class JpaOutboxPublisher implements OutboxPublisherPort {

  private final OutboxEventJpaRepository outboxRepo;
  private final ObjectMapper objectMapper;

  public JpaOutboxPublisher(OutboxEventJpaRepository outboxRepo, ObjectMapper objectMapper) {
    this.outboxRepo = outboxRepo;
    this.objectMapper = objectMapper;
  }

  @Override
  public void publish(
      String aggregateType, String aggregateId, String eventType, Map<String, String> payload) {
    OutboxEventEntity outbox = new OutboxEventEntity();
    outbox.setId(UUID.randomUUID());
    outbox.setAggregateType(aggregateType);
    outbox.setAggregateId(aggregateId);
    outbox.setEventType(eventType);
    outbox.setPayload(writeJson(payload));
    outbox.setStatus("PENDING");
    outbox.setAttempts(0);
    Instant now = Instant.now();
    outbox.setCreatedAt(now);
    outbox.setUpdatedAt(now);
    outboxRepo.save(outbox);
  }

  private String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      return "{}";
    }
  }
}
