package com.union.solutions.saascore.unit.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.union.solutions.saascore.config.OtlpTracingEnvironmentPostProcessor;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

class OtlpTracingEnvironmentPostProcessorTest {

  private static final String ENDPOINT = "management.opentelemetry.tracing.export.otlp.endpoint";
  private static final String EXPORT_ENABLED = "management.tracing.export.otlp.enabled";

  private StandardEnvironment ambienteCom(String endpoint) {
    StandardEnvironment env = new StandardEnvironment();
    env.getPropertySources().addFirst(new MapPropertySource("teste", Map.of(ENDPOINT, endpoint)));
    new OtlpTracingEnvironmentPostProcessor().postProcessEnvironment(env, null);
    return env;
  }

  @Test
  void endpointVazioDesligaAExportacao() {
    assertThat(ambienteCom("").getProperty(EXPORT_ENABLED)).isEqualTo("false");
    assertThat(ambienteCom("  ").getProperty(EXPORT_ENABLED)).isEqualTo("false");
  }

  @Test
  void endpointPreenchidoMantemAExportacao() {
    assertThat(ambienteCom("http://coletor:4318/v1/traces").getProperty(EXPORT_ENABLED)).isNull();
  }
}
