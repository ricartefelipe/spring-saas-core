package com.union.solutions.saascore.config;

import java.util.Map;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

/**
 * Com o endpoint OTLP vazio (OTEL_EXPORTER_OTLP_TRACES_ENDPOINT=""), o Spring Boot 4 ainda cria o
 * exportador de spans e falha ao iniciar. Endpoint vazio passa a significar exportação desligada,
 * sem desligar o rastreamento local (traceId e spanId nos logs).
 */
public class OtlpTracingEnvironmentPostProcessor implements EnvironmentPostProcessor {

  static final String ENDPOINT = "management.opentelemetry.tracing.export.otlp.endpoint";
  static final String EXPORT_ENABLED = "management.tracing.export.otlp.enabled";

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {
    if (environment.containsProperty(ENDPOINT)
        && !StringUtils.hasText(environment.getProperty(ENDPOINT))) {
      environment
          .getPropertySources()
          .addFirst(
              new MapPropertySource("otlpTracingSemEndpoint", Map.of(EXPORT_ENABLED, "false")));
    }
  }
}
