package com.union.solutions.saascore.application.port;

import java.util.Map;

public interface OutboxPublisherPort {

  void publish(
      String aggregateType, String aggregateId, String eventType, Map<String, String> payload);
}
