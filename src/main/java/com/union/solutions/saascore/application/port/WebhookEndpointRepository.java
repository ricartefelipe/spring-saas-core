package com.union.solutions.saascore.application.port;

import com.union.solutions.saascore.domain.WebhookEndpoint;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WebhookEndpointRepository {

  WebhookEndpoint save(WebhookEndpoint endpoint);

  Optional<WebhookEndpoint> findByIdAndTenantId(UUID id, UUID tenantId);

  List<WebhookEndpoint> findByTenantIdAndActiveTrue(UUID tenantId);

  boolean deleteByIdAndTenantId(UUID id, UUID tenantId);
}
