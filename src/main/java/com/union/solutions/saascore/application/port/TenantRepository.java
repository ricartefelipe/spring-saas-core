package com.union.solutions.saascore.application.port;

import com.union.solutions.saascore.domain.Tenant;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Port de persistência de tenants. A aplicação depende apenas desta interface. */
public interface TenantRepository {

  Tenant save(Tenant tenant);

  Optional<Tenant> findById(UUID id);

  Page<Tenant> search(
      Tenant.TenantStatus status, String plan, String region, String name, Pageable pageable);

  List<Tenant> findNextPage(
      Tenant.TenantStatus status,
      String plan,
      String region,
      String name,
      Instant cursor,
      Pageable pageable);

  long countByStatus(Tenant.TenantStatus status);

  /** Retorno: (plan, status.name(), count) para métricas. */
  List<PlanStatusCount> countByPlanAndStatus();

  record PlanStatusCount(String plan, String status, long count) {}
}
