package com.union.solutions.saascore.adapters.out.persistence;

import com.union.solutions.saascore.application.port.WebhookDeliveryRepository;
import com.union.solutions.saascore.domain.WebhookDelivery;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class WebhookDeliveryRepositoryAdapter implements WebhookDeliveryRepository {

  private final WebhookDeliveryJpaRepository jpa;

  public WebhookDeliveryRepositoryAdapter(WebhookDeliveryJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public WebhookDelivery save(WebhookDelivery delivery) {
    WebhookDeliveryEntity e = toEntity(delivery);
    WebhookDeliveryEntity saved = jpa.save(e);
    return toDomain(saved);
  }

  @Override
  public List<WebhookDelivery> findPendingReadyForDelivery(Instant now, Pageable pageable) {
    return jpa.findPendingReadyForDelivery(now, pageable).stream().map(this::toDomain).toList();
  }

  private WebhookDelivery toDomain(WebhookDeliveryEntity e) {
    return new WebhookDelivery(
        e.getId(),
        e.getEndpointId(),
        e.getTenantId(),
        e.getEventType(),
        e.getPayload(),
        WebhookDelivery.Status.valueOf(e.getStatus()),
        e.getAttempts(),
        e.getResponseCode(),
        e.getNextAttemptAt(),
        e.getCreatedAt(),
        e.getUpdatedAt());
  }

  private WebhookDeliveryEntity toEntity(WebhookDelivery d) {
    WebhookDeliveryEntity e = new WebhookDeliveryEntity();
    e.setId(d.getId());
    e.setEndpointId(d.getEndpointId());
    e.setTenantId(d.getTenantId());
    e.setEventType(d.getEventType());
    e.setPayload(d.getPayload());
    e.setStatus(d.getStatus().name());
    e.setAttempts(d.getAttempts());
    e.setResponseCode(d.getResponseCode());
    e.setNextAttemptAt(d.getNextAttemptAt());
    Instant now = Instant.now();
    e.setCreatedAt(d.getCreatedAt() != null ? d.getCreatedAt() : now);
    e.setUpdatedAt(d.getUpdatedAt() != null ? d.getUpdatedAt() : now);
    return e;
  }
}
