package com.union.solutions.saascore.infrastructure.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.union.solutions.saascore.application.port.WebhookDeliveryRepository;
import com.union.solutions.saascore.application.port.WebhookEndpointRepository;
import com.union.solutions.saascore.application.port.WebhookEnqueuerPort;
import com.union.solutions.saascore.domain.WebhookDelivery;
import com.union.solutions.saascore.domain.WebhookEndpoint;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class WebhookEnqueuer implements WebhookEnqueuerPort {

  private static final Logger log = LoggerFactory.getLogger(WebhookEnqueuer.class);
  private static final List<String> SENSITIVE_KEYS = List.of("rawToken");

  private final WebhookEndpointRepository endpointRepo;
  private final WebhookDeliveryRepository deliveryRepo;
  private final ObjectMapper objectMapper;

  public WebhookEnqueuer(
      WebhookEndpointRepository endpointRepo,
      WebhookDeliveryRepository deliveryRepo,
      ObjectMapper objectMapper) {
    this.endpointRepo = endpointRepo;
    this.deliveryRepo = deliveryRepo;
    this.objectMapper = objectMapper;
  }

  @Override
  @Transactional
  public void enqueue(UUID tenantId, String eventType, String payload) {
    if (tenantId == null) {
      return;
    }
    String sanitizedPayload = sanitizePayload(payload);
    List<WebhookEndpoint> endpoints = endpointRepo.findByTenantIdAndActiveTrue(tenantId);
    for (WebhookEndpoint endpoint : endpoints) {
      if (!endpoint.subscribesTo(eventType)) {
        continue;
      }
      WebhookDelivery delivery =
          new WebhookDelivery(
              UUID.randomUUID(),
              endpoint.getId(),
              tenantId,
              eventType,
              sanitizedPayload,
              WebhookDelivery.Status.PENDING,
              0,
              null,
              null,
              Instant.now(),
              Instant.now());
      deliveryRepo.save(delivery);
      log.debug(
          "Webhook delivery enqueued endpoint_id={} event_type={} tenant_id={}",
          endpoint.getId(),
          eventType,
          tenantId);
    }
  }

  private String sanitizePayload(String payload) {
    try {
      JsonNode node = objectMapper.readTree(payload);
      if (node instanceof ObjectNode obj) {
        for (String key : SENSITIVE_KEYS) {
          obj.remove(key);
        }
        return objectMapper.writeValueAsString(obj);
      }
      return payload;
    } catch (Exception ex) {
      return payload;
    }
  }
}
