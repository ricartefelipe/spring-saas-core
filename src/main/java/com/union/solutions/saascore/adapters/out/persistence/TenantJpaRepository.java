package com.union.solutions.saascore.adapters.out.persistence;

import com.union.solutions.saascore.domain.Tenant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TenantJpaRepository extends JpaRepository<TenantEntity, UUID> {

  @Query(
      """
        SELECT t FROM TenantEntity t
        WHERE (:status IS NULL OR t.status = :status)
        AND (:plan IS NULL OR t.plan = :plan)
        AND (:region IS NULL OR t.region = :region)
        AND (:name = '' OR LOWER(t.name) LIKE LOWER(CONCAT('%', :name, '%')))
        ORDER BY t.createdAt DESC
    """)
  Page<TenantEntity> search(
      @Param("status") Tenant.TenantStatus status,
      @Param("plan") String plan,
      @Param("region") String region,
      @Param("name") String name,
      Pageable pageable);

  @Query(
      """
        SELECT t FROM TenantEntity t
        WHERE (:status IS NULL OR t.status = :status)
        AND (:plan IS NULL OR t.plan = :plan)
        AND (:region IS NULL OR t.region = :region)
        AND (:name = '' OR LOWER(t.name) LIKE LOWER(CONCAT('%', :name, '%')))
        AND t.createdAt > :cursor
        ORDER BY t.createdAt ASC
    """)
  List<TenantEntity> findNextPage(
      @Param("status") Tenant.TenantStatus status,
      @Param("plan") String plan,
      @Param("region") String region,
      @Param("name") String name,
      @Param("cursor") java.time.Instant cursor,
      Pageable pageable);

  long countByStatus(Tenant.TenantStatus status);

  @Query("SELECT t.plan, t.status, COUNT(t) FROM TenantEntity t GROUP BY t.plan, t.status")
  List<Object[]> countByPlanAndStatus();

  @Query("SELECT t.plan, COUNT(t) FROM TenantEntity t GROUP BY t.plan")
  List<Object[]> countGroupByPlan();

  @Query("SELECT t.status, COUNT(t) FROM TenantEntity t GROUP BY t.status")
  List<Object[]> countGroupByStatus();

  @Query("SELECT t.region, COUNT(t) FROM TenantEntity t GROUP BY t.region")
  List<Object[]> countGroupByRegion();
}
