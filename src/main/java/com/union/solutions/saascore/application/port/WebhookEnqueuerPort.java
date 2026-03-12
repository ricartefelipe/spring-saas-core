package com.union.solutions.saascore.application.port;

import java.util.Map;
import java.util.UUID;

public interface WebhookEnqueuerPort {

  /**
   * Enqueues webhook deliveries for all endpoints of the tenant that subscribe to the event.
   *
   * @param tenantId tenant context (null skips enqueue)
   * @param eventType event type (e.g. user.registered)
   * @param payload JSON payload
   */
  void enqueue(UUID tenantId, String eventType, String payload);
}
