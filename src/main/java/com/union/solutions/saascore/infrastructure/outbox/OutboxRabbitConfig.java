package com.union.solutions.saascore.infrastructure.outbox;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.outbox.publish-enabled", havingValue = "true")
public class OutboxRabbitConfig {

  @Value("${app.outbox.exchange:saas.events}")
  private String exchangeName;

  @Bean
  public TopicExchange saasEventsExchange() {
    return new TopicExchange(exchangeName, true, false);
  }

  @Bean
  public Queue outboxQueue() {
    return new Queue("saas.outbox.events", true, false, false);
  }

  @Bean
  public Binding outboxBinding(Queue outboxQueue, TopicExchange saasEventsExchange) {
    return BindingBuilder.bind(outboxQueue).to(saasEventsExchange).with("saas.#");
  }
}
