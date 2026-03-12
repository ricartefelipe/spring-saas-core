package com.union.solutions.saascore.adapters.out.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, UUID> {

  @Query(
      """
        SELECT e FROM OutboxEventEntity e
        WHERE e.status = :status
        ORDER BY e.createdAt ASC
    """)
  List<OutboxEventEntity> findByStatus(@Param("status") String status, Pageable pageable);

  @Query(
      """
        SELECT e FROM OutboxEventEntity e
        WHERE e.status = 'PENDING'
          AND (e.nextAttemptAt IS NULL OR e.nextAttemptAt <= :now)
        ORDER BY e.createdAt ASC
    """)
  List<OutboxEventEntity> findPendingReadyForDispatch(
      @Param("now") java.time.Instant now, Pageable pageable);
}
