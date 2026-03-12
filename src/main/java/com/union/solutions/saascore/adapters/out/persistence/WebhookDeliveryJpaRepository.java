package com.union.solutions.saascore.adapters.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WebhookDeliveryJpaRepository extends JpaRepository<WebhookDeliveryEntity, UUID> {

  @Query(
      """
        SELECT d FROM WebhookDeliveryEntity d
        WHERE d.status = 'PENDING'
          AND (d.nextAttemptAt IS NULL OR d.nextAttemptAt <= :now)
        ORDER BY d.createdAt ASC
    """)
  List<WebhookDeliveryEntity> findPendingReadyForDelivery(
      @Param("now") Instant now, Pageable pageable);
}
