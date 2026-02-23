package com.union.solutions.saascore.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

  @Bean
  public Counter tenantsCreatedCounter(MeterRegistry registry) {
    return Counter.builder("saas_tenants_created_total")
        .description("Total tenants created")
        .register(registry);
  }

  @Bean
  public Counter policiesUpdatedCounter(MeterRegistry registry) {
    return Counter.builder("saas_policies_updated_total")
        .description("Total policies updated")
        .register(registry);
  }

  @Bean
  public Counter flagsToggledCounter(MeterRegistry registry) {
    return Counter.builder("saas_flags_toggled_total")
        .description("Total feature flags toggled")
        .register(registry);
  }

  @Bean
  public Counter accessDeniedCounter(MeterRegistry registry) {
    return Counter.builder("saas_access_denied_total")
        .description("Total access denied events")
        .register(registry);
  }

  @Bean
  public Counter outboxPublishedCounter(MeterRegistry registry) {
    return Counter.builder("saas_outbox_published_total")
        .description("Total outbox events published to message broker")
        .register(registry);
  }

  @Bean
  public Counter outboxFailedCounter(MeterRegistry registry) {
    return Counter.builder("saas_outbox_failed_total")
        .description("Total outbox events failed after max retries")
        .register(registry);
  }
}
