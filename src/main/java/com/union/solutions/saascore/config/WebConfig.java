package com.union.solutions.saascore.config;

import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  private static final Logger log = LoggerFactory.getLogger(WebConfig.class);

  private final String[] allowedOrigins;

  public WebConfig(@Value("${app.cors.allowed-origins:*}") String origins) {
    this.allowedOrigins =
        Arrays.stream(origins.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toArray(String[]::new);
  }

  @Override
  public void addCorsMappings(@NonNull CorsRegistry registry) {
    if (allowedOrigins.length == 0) {
      log.warn(
          "app.cors.allowed-origins is empty — CORS defaults to localhost only. "
              + "Set CORS_ALLOWED_ORIGINS in production.");
    }
    String[] origins =
        allowedOrigins.length > 0 ? allowedOrigins : new String[] {"http://localhost:4200"};
    registry
        .addMapping("/**")
        .allowedOrigins(origins)
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .exposedHeaders(
            "X-Correlation-Id", "X-RateLimit-Limit", "X-RateLimit-Remaining", "Retry-After");
  }
}
