package com.union.solutions.saascore.unit.infrastructure.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.union.solutions.saascore.adapters.out.persistence.OutboxEventEntity;
import com.union.solutions.saascore.adapters.out.persistence.OutboxEventJpaRepository;
import com.union.solutions.saascore.infrastructure.outbox.OutboxPublisher;
import com.union.solutions.saascore.infrastructure.outbox.RabbitOutboxSender;
import io.micrometer.core.instrument.Counter;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

  @Mock OutboxEventJpaRepository outboxRepo;
  @Mock RabbitOutboxSender rabbitOutboxSender;
  @Mock Tracer tracer;
  @Mock Counter publishedCounter;
  @Mock Counter failedCounter;
  @Mock Span mockSpan;
  @Mock Tracer.SpanInScope mockSpanInScope;
  @Mock TraceContext mockTraceContext;

  private OutboxPublisher publisher;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    lenient().when(tracer.nextSpan()).thenReturn(mockSpan);
    lenient().when(mockSpan.name(anyString())).thenReturn(mockSpan);
    lenient().when(mockSpan.tag(anyString(), anyString())).thenReturn(mockSpan);
    lenient().when(mockSpan.start()).thenReturn(mockSpan);
    lenient().when(tracer.withSpan(mockSpan)).thenReturn(mockSpanInScope);
    lenient().when(mockSpan.context()).thenReturn(mockTraceContext);
    lenient().when(mockTraceContext.traceId()).thenReturn("00000000000000000000000000000001");
    lenient().when(mockTraceContext.spanId()).thenReturn("0000000000000002");

    publisher =
        new OutboxPublisher(
            outboxRepo,
            rabbitOutboxSender,
            objectMapper,
            tracer,
            publishedCounter,
            failedCounter,
            50,
            3,
            60,
            "saas.events",
            "saas");
  }

  @Test
  void publishPending_sendsToRabbitAndMarksPublished() {
    OutboxEventEntity event = makeEvent("tenant", "tenant.created");

    when(outboxRepo.findPendingReadyForDispatch(any(Instant.class), any(Pageable.class)))
        .thenReturn(List.of(event));

    publisher.publishPending();

    verify(rabbitOutboxSender)
        .send(eq("saas.events"), eq("saas.tenant.tenant.created"), anyString(), any(Map.class));
    assertThat(event.getStatus()).isEqualTo("PUBLISHED");
    verify(publishedCounter).increment();
    verify(outboxRepo).save(event);
  }

  @Test
  void publishPending_noPendingEvents_doesNothing() {
    when(outboxRepo.findPendingReadyForDispatch(any(Instant.class), any(Pageable.class)))
        .thenReturn(List.of());

    publisher.publishPending();

    verify(rabbitOutboxSender, never()).send(anyString(), anyString(), anyString(), any(Map.class));
  }

  @Test
  void publishPending_onFailure_incrementsAttemptsAndRetries() {
    OutboxEventEntity event = makeEvent("policy", "policy.created");
    event.setAttempts(0);

    when(outboxRepo.findPendingReadyForDispatch(any(Instant.class), any(Pageable.class)))
        .thenReturn(List.of(event));
    doThrow(new RuntimeException("Connection refused"))
        .when(rabbitOutboxSender)
        .send(anyString(), anyString(), anyString(), any(Map.class));

    publisher.publishPending();

    assertThat(event.getAttempts()).isEqualTo(1);
    assertThat(event.getStatus()).isEqualTo("PENDING");
    assertThat(event.getNextAttemptAt()).isNotNull();
    verify(failedCounter, never()).increment();
  }

  @Test
  void publishPending_onMaxRetries_marksFailedAndIncrementsCounter() {
    OutboxEventEntity event = makeEvent("flag", "flag.created");
    event.setAttempts(2);

    when(outboxRepo.findPendingReadyForDispatch(any(Instant.class), any(Pageable.class)))
        .thenReturn(List.of(event));
    doThrow(new RuntimeException("Connection refused"))
        .when(rabbitOutboxSender)
        .send(anyString(), anyString(), anyString(), any(Map.class));

    publisher.publishPending();

    assertThat(event.getAttempts()).isEqualTo(3);
    assertThat(event.getStatus()).isEqualTo("FAILED");
    verify(failedCounter).increment();
  }

  @Test
  void publishPending_multipleEvents_publishesAll() {
    OutboxEventEntity e1 = makeEvent("tenant", "tenant.created");
    OutboxEventEntity e2 = makeEvent("policy", "policy.updated");

    when(outboxRepo.findPendingReadyForDispatch(any(Instant.class), any(Pageable.class)))
        .thenReturn(List.of(e1, e2));

    publisher.publishPending();

    verify(rabbitOutboxSender)
        .send(eq("saas.events"), eq("saas.tenant.tenant.created"), anyString(), any(Map.class));
    verify(rabbitOutboxSender)
        .send(eq("saas.events"), eq("saas.policy.policy.updated"), anyString(), any(Map.class));
    assertThat(e1.getStatus()).isEqualTo("PUBLISHED");
    assertThat(e2.getStatus()).isEqualTo("PUBLISHED");
    verify(publishedCounter, org.mockito.Mockito.times(2)).increment();
  }

  private static OutboxEventEntity makeEvent(String aggregateType, String eventType) {
    OutboxEventEntity e = new OutboxEventEntity();
    e.setId(UUID.randomUUID());
    e.setAggregateType(aggregateType);
    e.setAggregateId(UUID.randomUUID().toString());
    e.setEventType(eventType);
    e.setPayload("{\"key\":\"value\"}");
    e.setStatus("PENDING");
    e.setAttempts(0);
    e.setCreatedAt(Instant.now());
    e.setUpdatedAt(Instant.now());
    return e;
  }
}
