package com.union.solutions.saascore.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.union.solutions.saascore.adapters.out.persistence.OutboxEventEntity;
import com.union.solutions.saascore.adapters.out.persistence.OutboxEventJpaRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.outbox.publish-enabled", havingValue = "true")
public class OutboxPublisher {

  private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

  private final OutboxEventJpaRepository outboxRepo;
  private final RabbitOutboxSender rabbitOutboxSender;
  private final ObjectMapper objectMapper;
  private final Tracer tracer;
  private final Counter publishedCounter;
  private final Counter failedCounter;
  private final int batchSize;
  private final int retryMax;
  private final int lockTtlSeconds;
  private final String exchange;
  private final String routingKeyPrefix;

  public OutboxPublisher(
      OutboxEventJpaRepository outboxRepo,
      RabbitOutboxSender rabbitOutboxSender,
      ObjectMapper objectMapper,
      Tracer tracer,
      @Qualifier("outboxPublishedCounter") Counter publishedCounter,
      @Qualifier("outboxFailedCounter") Counter failedCounter,
      @Value("${app.outbox.batch-size:50}") int batchSize,
      @Value("${app.outbox.retry-max:5}") int retryMax,
      @Value("${app.outbox.lock-ttl-seconds:60}") int lockTtlSeconds,
      @Value("${app.outbox.exchange:saas.events}") String exchange,
      @Value("${app.outbox.routing-key-prefix:saas}") String routingKeyPrefix) {
    this.outboxRepo = outboxRepo;
    this.rabbitOutboxSender = rabbitOutboxSender;
    this.objectMapper = objectMapper;
    this.tracer = tracer;
    this.publishedCounter = publishedCounter;
    this.failedCounter = failedCounter;
    this.batchSize = batchSize;
    this.retryMax = retryMax;
    this.lockTtlSeconds = lockTtlSeconds;
    this.exchange = exchange;
    this.routingKeyPrefix = routingKeyPrefix;
  }

  @Scheduled(fixedDelayString = "${app.outbox.dispatch-interval-ms:5000}")
  @Transactional
  public void publishPending() {
    List<OutboxEventEntity> pending =
        outboxRepo.findPendingReadyForDispatch(Instant.now(), PageRequest.of(0, batchSize));
    for (OutboxEventEntity e : pending) {
      publishOne(e);
    }
  }

  private void publishOne(OutboxEventEntity e) {
    String routingKey = routingKeyPrefix + "." + e.getAggregateType() + "." + e.getEventType();

    Span outboxSpan =
        tracer
            .nextSpan()
            .name("outbox.publish " + e.getEventType())
            .tag("messaging.system", "rabbitmq")
            .tag("messaging.destination", exchange)
            .tag("messaging.routing_key", routingKey)
            .tag("outbox.event_id", e.getId().toString())
            .tag("outbox.aggregate_type", e.getAggregateType())
            .tag("outbox.aggregate_id", e.getAggregateId())
            .tag("outbox.event_type", e.getEventType())
            .start();

    try (Tracer.SpanInScope ws = tracer.withSpan(outboxSpan)) {
      Map<String, Object> envelope =
          Map.of(
              "id", e.getId().toString(),
              "aggregateType", e.getAggregateType(),
              "aggregateId", e.getAggregateId(),
              "eventType", e.getEventType(),
              "payload", parsePayload(e.getPayload()),
              "createdAt", e.getCreatedAt().toString());

      String body = objectMapper.writeValueAsString(envelope);

      Map<String, String> traceHeaders = new HashMap<>();
      traceHeaders.put("traceparent", buildTraceparent(outboxSpan));
      traceHeaders.put("X-Correlation-Id", e.getId().toString());

      rabbitOutboxSender.send(exchange, routingKey, body, traceHeaders);

      e.setStatus("PUBLISHED");
      e.setUpdatedAt(Instant.now());
      outboxRepo.save(e);
      publishedCounter.increment();
      log.info(
          "Outbox event published event_id={} aggregate_type={} event_type={} routing_key={}",
          e.getId(),
          e.getAggregateType(),
          e.getEventType(),
          routingKey);
    } catch (Exception ex) {
      outboxSpan.error(ex);
      handlePublishFailure(e, ex);
    } finally {
      outboxSpan.end();
    }
  }

  private String buildTraceparent(Span span) {
    return "00-" + span.context().traceId() + "-" + span.context().spanId() + "-01";
  }

  private void handlePublishFailure(OutboxEventEntity e, Exception ex) {
    int attempts = e.getAttempts() + 1;
    e.setAttempts(attempts);
    e.setUpdatedAt(Instant.now());
    boolean isFinal = attempts >= retryMax;
    e.setStatus(isFinal ? "FAILED" : "PENDING");
    e.setNextAttemptAt(Instant.now().plusSeconds(lockTtlSeconds));
    outboxRepo.save(e);
    if (isFinal) {
      failedCounter.increment();
      log.error(
          "Outbox event failed permanently event_id={} aggregate_type={} event_type={} attempts={} max_retries={} error={}",
          e.getId(),
          e.getAggregateType(),
          e.getEventType(),
          attempts,
          retryMax,
          ex.getMessage());
    } else {
      log.warn(
          "Outbox event publish failed, will retry event_id={} attempt={} max_retries={} error={}",
          e.getId(),
          attempts,
          retryMax,
          ex.getMessage());
    }
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> parsePayload(String payload) {
    try {
      return objectMapper.readValue(payload, Map.class);
    } catch (Exception ex) {
      return Map.of("raw", payload);
    }
  }
}
