package com.union.solutions.saascore.config;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

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
    String[] origins = allowedOrigins.length > 0 ? allowedOrigins : new String[] {"*"};
    registry
        .addMapping("/**")
        .allowedOrigins(origins)
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .exposedHeaders(
            "X-Correlation-Id", "X-RateLimit-Limit", "X-RateLimit-Remaining", "Retry-After");
  }
}
