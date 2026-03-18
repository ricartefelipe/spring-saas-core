package com.union.solutions.saascore.adapters.out.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reactivation_email_sent")
public class ReactivationEmailSentEntity {

  @Id
  @Column(name = "tenant_id", nullable = false)
  private UUID tenantId;

  @Column(name = "sent_at", nullable = false)
  private Instant sentAt;

  public static ReactivationEmailSentEntity of(UUID tenantId, Instant sentAt) {
    ReactivationEmailSentEntity e = new ReactivationEmailSentEntity();
    e.tenantId = tenantId;
    e.sentAt = sentAt != null ? sentAt : Instant.now();
    return e;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public Instant getSentAt() {
    return sentAt;
  }

  public void setSentAt(Instant sentAt) {
    this.sentAt = sentAt;
  }
}
