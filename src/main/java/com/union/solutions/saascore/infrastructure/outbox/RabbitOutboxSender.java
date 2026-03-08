package com.union.solutions.saascore.infrastructure.outbox;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Delegates RabbitMQ publish with circuit breaker protection. Used by OutboxPublisher to fail-fast
 * when Rabbit is unavailable.
 */
@Component
@ConditionalOnProperty(name = "app.outbox.publish-enabled", havingValue = "true")
public class RabbitOutboxSender {

  private static final Logger log = LoggerFactory.getLogger(RabbitOutboxSender.class);

  private final RabbitTemplate rabbitTemplate;

  public RabbitOutboxSender(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  @CircuitBreaker(name = "rabbitPublisher", fallbackMethod = "sendFallback")
  public void send(String exchange, String routingKey, String body) {
    rabbitTemplate.convertAndSend(exchange, routingKey, body);
  }

  @SuppressWarnings("unused")
  public void sendFallback(String exchange, String routingKey, String body, Throwable t) {
    log.warn(
        "Rabbit outbox publish circuit open or error (exchange={}, routingKey={}): {}",
        exchange,
        routingKey,
        t.getMessage());
    throw new org.springframework.amqp.AmqpException(
        "Rabbit outbox publish unavailable: " + t.getMessage(), t);
  }
}
