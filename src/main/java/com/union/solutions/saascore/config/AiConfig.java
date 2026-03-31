package com.union.solutions.saascore.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AiConfig {

  @Bean
  @ConfigurationProperties(prefix = "app.ai")
  public AiProperties aiProperties() {
    return new AiProperties();
  }

  @Bean("aiRestClient")
  public RestClient aiRestClient(AiProperties props) {
    return RestClient.builder()
        .baseUrl(props.getBaseUrl())
        .defaultHeader("Authorization", "Bearer " + props.getApiKey())
        .defaultHeader("Content-Type", "application/json")
        .build();
  }

  public static class AiProperties {
    private boolean enabled = false;
    private String provider = "openai";
    private String apiKey = "";
    private String baseUrl = "https://api.openai.com/v1";
    private String model = "gpt-4o-mini";
    private int maxTokens = 2048;
    private double temperature = 0.3;
    private int timeoutSeconds = 30;

    public boolean isEnabled() {
      return enabled && apiKey != null && !apiKey.isBlank();
    }

    /** Expõe no /v1/ai/status se a chave está definida (sem revelar o valor). */
    public boolean hasApiKey() {
      return apiKey != null && !apiKey.isBlank();
    }

    public String getProvider() {
      return provider;
    }

    public void setProvider(String provider) {
      this.provider = provider;
    }

    public String getApiKey() {
      return apiKey;
    }

    public void setApiKey(String apiKey) {
      this.apiKey = apiKey;
    }

    public String getBaseUrl() {
      return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
    }

    public String getModel() {
      return model;
    }

    public void setModel(String model) {
      this.model = model;
    }

    public int getMaxTokens() {
      return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
      this.maxTokens = maxTokens;
    }

    public double getTemperature() {
      return temperature;
    }

    public void setTemperature(double temperature) {
      this.temperature = temperature;
    }

    public int getTimeoutSeconds() {
      return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
      this.timeoutSeconds = timeoutSeconds;
    }

    public boolean getEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }
  }
}
