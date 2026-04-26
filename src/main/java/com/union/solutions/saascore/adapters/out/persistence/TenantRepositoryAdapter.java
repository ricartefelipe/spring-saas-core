package com.union.solutions.saascore.adapters.out.persistence;

import com.union.solutions.saascore.application.port.TenantRepository;
import com.union.solutions.saascore.domain.Tenant;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class TenantRepositoryAdapter implements TenantRepository {

  private final TenantJpaRepository jpa;

  public TenantRepositoryAdapter(TenantJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public Tenant save(Tenant tenant) {
    TenantEntity e = TenantEntity.from(tenant);
    TenantEntity saved = jpa.save(e);
    return saved.toDomain();
  }

  @Override
  public Optional<Tenant> findById(UUID id) {
    if (id == null) {
      return Optional.empty();
    }
    return jpa.findById(id).map(TenantEntity::toDomain);
  }

  @Override
  public org.springframework.data.domain.Page<Tenant> search(
      Tenant.TenantStatus status, String plan, String region, String name, Pageable pageable) {
    String safeName = (name == null || name.isBlank()) ? "" : name;
    return jpa.search(status, plan, region, safeName, pageable).map(TenantEntity::toDomain);
  }

  @Override
  public List<Tenant> findNextPage(
      Tenant.TenantStatus status,
      String plan,
      String region,
      String name,
      Instant cursor,
      Pageable pageable) {
    String safeName = (name == null || name.isBlank()) ? "" : name;
    return jpa.findNextPage(status, plan, region, safeName, cursor, pageable).stream()
        .map(TenantEntity::toDomain)
        .toList();
  }

  @Override
  public long countByStatus(Tenant.TenantStatus status) {
    return jpa.countByStatus(status);
  }

  @Override
  public List<PlanStatusCount> countByPlanAndStatus() {
    return jpa.countByPlanAndStatus().stream()
        .map(
            row ->
                new PlanStatusCount(
                    (String) row[0], ((Tenant.TenantStatus) row[1]).name(), (Long) row[2]))
        .toList();
  }

  @Override
  public List<UUID> findAllActiveIds() {
    return jpa.findAllActiveIds();
  }
}
