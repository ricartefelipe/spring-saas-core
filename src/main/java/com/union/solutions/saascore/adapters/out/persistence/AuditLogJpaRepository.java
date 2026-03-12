package com.union.solutions.saascore.adapters.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogJpaRepository extends JpaRepository<AuditLogEntity, UUID> {

  @Query(
      """
        SELECT a FROM AuditLogEntity a
        WHERE (:tenantId IS NULL OR a.tenantId = :tenantId)
        AND (:action = '' OR a.action = :action)
        AND (:actorSub = '' OR a.actorSub = :actorSub)
        AND (:correlationId = '' OR a.correlationId = :correlationId)
        AND a.createdAt >= :from AND a.createdAt <= :to
        ORDER BY a.createdAt DESC
    """)
  Page<AuditLogEntity> search(
      @Param("tenantId") UUID tenantId,
      @Param("action") String action,
      @Param("actorSub") String actorSub,
      @Param("correlationId") String correlationId,
      @Param("from") Instant from,
      @Param("to") Instant to,
      Pageable pageable);

  @Query(
      """
        SELECT a FROM AuditLogEntity a
        WHERE (:tenantId IS NULL OR a.tenantId = :tenantId)
        AND (:action = '' OR a.action = :action)
        AND (:actorSub = '' OR a.actorSub = :actorSub)
        AND (:correlationId = '' OR a.correlationId = :correlationId)
        AND a.createdAt >= :from AND a.createdAt <= :to
        AND a.createdAt < :cursor
        ORDER BY a.createdAt DESC
    """)
  List<AuditLogEntity> findNextPage(
      @Param("tenantId") UUID tenantId,
      @Param("action") String action,
      @Param("actorSub") String actorSub,
      @Param("correlationId") String correlationId,
      @Param("from") Instant from,
      @Param("to") Instant to,
      @Param("cursor") Instant cursor,
      Pageable pageable);

  @Modifying
  @Query(
      value =
          "DELETE FROM audit_log WHERE id IN "
              + "(SELECT id FROM audit_log WHERE created_at < :cutoff LIMIT :batchSize)",
      nativeQuery = true)
  int deleteBatchOlderThan(@Param("cutoff") Instant cutoff, @Param("batchSize") int batchSize);

  @Query("SELECT COUNT(a) FROM AuditLogEntity a WHERE a.createdAt >= :since")
  long countSince(@Param("since") Instant since);

  @Query(
      value =
          "SELECT a.action, COUNT(*) as cnt FROM audit_log a"
              + " WHERE a.created_at >= :since"
              + " GROUP BY a.action ORDER BY cnt DESC LIMIT :limit",
      nativeQuery = true)
  List<Object[]> topActionsSince(@Param("since") Instant since, @Param("limit") int limit);

  @Query(
      value =
          "SELECT actor_sub, CAST(tenant_id AS text), COUNT(*) as cnt,"
              + " MIN(created_at) as window_start, MAX(created_at) as window_end"
              + " FROM audit_log"
              + " WHERE created_at >= :since"
              + " GROUP BY actor_sub, tenant_id,"
              + " CAST(EXTRACT(EPOCH FROM created_at) AS bigint) / 300"
              + " HAVING COUNT(*) > :threshold"
              + " ORDER BY cnt DESC LIMIT 50",
      nativeQuery = true)
  List<Object[]> findBurstAccess(@Param("since") Instant since, @Param("threshold") int threshold);

  @Query(
      value =
          "SELECT actor_sub, COUNT(*) as cnt,"
              + " MIN(created_at) as window_start, MAX(created_at) as window_end"
              + " FROM audit_log"
              + " WHERE created_at >= :since AND status_code = 403"
              + " GROUP BY actor_sub,"
              + " CAST(EXTRACT(EPOCH FROM created_at) AS bigint) / 3600"
              + " HAVING COUNT(*) > :threshold"
              + " ORDER BY cnt DESC LIMIT 50",
      nativeQuery = true)
  List<Object[]> findAccessDeniedSpikes(
      @Param("since") Instant since, @Param("threshold") int threshold);

  @Query(
      value =
          "SELECT actor_sub, CAST(tenant_id AS text), COUNT(*) as cnt,"
              + " MIN(created_at) as first_at, MAX(created_at) as last_at"
              + " FROM audit_log"
              + " WHERE created_at >= :since"
              + " AND EXTRACT(HOUR FROM created_at) >= :startHour"
              + " AND EXTRACT(HOUR FROM created_at) < :endHour"
              + " GROUP BY actor_sub, tenant_id"
              + " ORDER BY cnt DESC LIMIT 50",
      nativeQuery = true)
  List<Object[]> findOffHoursActivity(
      @Param("since") Instant since,
      @Param("startHour") int startHour,
      @Param("endHour") int endHour);

  @Query(
      value =
          "SELECT actor_sub, COUNT(DISTINCT tenant_id) as tenant_count,"
              + " MIN(created_at) as window_start, MAX(created_at) as window_end"
              + " FROM audit_log"
              + " WHERE created_at >= :since"
              + " GROUP BY actor_sub,"
              + " CAST(EXTRACT(EPOCH FROM created_at) AS bigint) / 3600"
              + " HAVING COUNT(DISTINCT tenant_id) > :threshold"
              + " ORDER BY tenant_count DESC LIMIT 50",
      nativeQuery = true)
  List<Object[]> findUnusualTenantSwitching(
      @Param("since") Instant since, @Param("threshold") int threshold);
}
