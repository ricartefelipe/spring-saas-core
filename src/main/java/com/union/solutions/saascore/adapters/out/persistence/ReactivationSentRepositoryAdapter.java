package com.union.solutions.saascore.adapters.out.persistence;

import com.union.solutions.saascore.application.port.ReactivationSentRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ReactivationSentRepositoryAdapter implements ReactivationSentRepository {

  private final ReactivationEmailSentJpaRepository jpa;

  public ReactivationSentRepositoryAdapter(ReactivationEmailSentJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public void record(UUID tenantId, Instant sentAt) {
    Instant at = sentAt != null ? sentAt : Instant.now();
    ReactivationEmailSentEntity e =
        jpa.findById(tenantId).orElse(ReactivationEmailSentEntity.of(tenantId, at));
    e.setSentAt(at);
    jpa.save(e);
  }

  @Override
  public boolean wasSentAfter(UUID tenantId, Instant after) {
    return jpa.existsByTenantIdAndSentAtAfter(tenantId, after);
  }
}
