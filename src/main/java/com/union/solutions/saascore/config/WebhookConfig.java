package com.union.solutions.saascore.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class WebhookConfig {

  @Bean
  public RestTemplate webhookRestTemplate(RestTemplateBuilder builder) {
    return builder
        .setConnectTimeout(java.time.Duration.ofSeconds(10))
        .setReadTimeout(java.time.Duration.ofSeconds(30))
        .build();
  }
}
