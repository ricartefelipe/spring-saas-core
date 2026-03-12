package com.union.solutions.saascore.infrastructure.outbox;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

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

  @CircuitBreaker(name = "rabbitPublisher", fallbackMethod = "sendWithHeadersFallback")
  public void send(String exchange, String routingKey, String body, Map<String, String> headers) {
    MessagePostProcessor postProcessor =
        message -> {
          headers.forEach(
              (k, v) -> message.getMessageProperties().setHeader(k, v));
          return message;
        };
    rabbitTemplate.convertAndSend(exchange, routingKey, body, postProcessor);
  }

  @SuppressWarnings("unused")
  public void sendFallback(String exchange, String routingKey, String body, Throwable t) {
    logAndThrow(exchange, routingKey, t);
  }

  @SuppressWarnings("unused")
  public void sendWithHeadersFallback(
      String exchange, String routingKey, String body, Map<String, String> headers, Throwable t) {
    logAndThrow(exchange, routingKey, t);
  }

  private void logAndThrow(String exchange, String routingKey, Throwable t) {
    log.warn(
        "Rabbit outbox publish circuit open or error (exchange={}, routingKey={}): {}",
        exchange,
        routingKey,
        t.getMessage());
    throw new org.springframework.amqp.AmqpException(
        "Rabbit outbox publish unavailable: " + t.getMessage(), t);
  }
}
