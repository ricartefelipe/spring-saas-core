package com.union.solutions.saascore.application.port;

import java.time.Instant;
import java.util.UUID;

public interface ReactivationSentRepository {

  void record(UUID tenantId, Instant sentAt);

  boolean wasSentAfter(UUID tenantId, Instant after);
}
