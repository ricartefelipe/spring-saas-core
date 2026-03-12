package com.union.solutions.saascore.adapters.out.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookEndpointJpaRepository extends JpaRepository<WebhookEndpointEntity, UUID> {

  List<WebhookEndpointEntity> findByTenantIdAndActiveTrue(UUID tenantId);
}
