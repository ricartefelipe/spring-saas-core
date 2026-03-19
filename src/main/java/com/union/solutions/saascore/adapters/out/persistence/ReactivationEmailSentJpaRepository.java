package com.union.solutions.saascore.adapters.out.persistence;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReactivationEmailSentJpaRepository
    extends JpaRepository<ReactivationEmailSentEntity, UUID> {

  @Query(
      "SELECT COUNT(r) > 0 FROM ReactivationEmailSentEntity r WHERE r.tenantId = :tenantId AND"
          + " r.sentAt > :after")
  boolean existsByTenantIdAndSentAtAfter(
      @Param("tenantId") UUID tenantId, @Param("after") Instant after);
}
