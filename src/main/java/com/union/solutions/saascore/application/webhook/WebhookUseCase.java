package com.union.solutions.saascore.application.webhook;

import com.union.solutions.saascore.application.port.WebhookEndpointRepository;
import com.union.solutions.saascore.domain.WebhookEndpoint;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebhookUseCase {

  private final WebhookEndpointRepository endpointRepo;

  public WebhookUseCase(WebhookEndpointRepository endpointRepo) {
    this.endpointRepo = endpointRepo;
  }

  @Transactional
  public WebhookEndpoint register(UUID tenantId, String url, String secret, List<String> events) {
    WebhookEndpoint endpoint =
        new WebhookEndpoint(
            UUID.randomUUID(),
            tenantId,
            url,
            secret,
            events != null ? events : List.of(),
            true,
            Instant.now(),
            Instant.now());
    return endpointRepo.save(endpoint);
  }

  @Transactional(readOnly = true)
  public List<WebhookEndpoint> listByTenant(UUID tenantId) {
    return endpointRepo.findByTenantIdAndActiveTrue(tenantId);
  }

  @Transactional
  public boolean delete(UUID id, UUID tenantId) {
    return endpointRepo.deleteByIdAndTenantId(id, tenantId);
  }

  @Transactional(readOnly = true)
  public Optional<WebhookEndpoint> getById(UUID id, UUID tenantId) {
    return endpointRepo.findByIdAndTenantId(id, tenantId);
  }
}
