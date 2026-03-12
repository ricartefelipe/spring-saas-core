package com.union.solutions.saascore.adapters.out.persistence;

import com.union.solutions.saascore.application.port.PlanDefinitionRepository;
import com.union.solutions.saascore.domain.PlanDefinition;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PlanDefinitionRepositoryAdapter implements PlanDefinitionRepository {

  private final PlanDefinitionJpaRepository jpa;

  public PlanDefinitionRepositoryAdapter(PlanDefinitionJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public PlanDefinition save(PlanDefinition plan) {
    return jpa.save(PlanDefinitionEntity.fromDomain(plan)).toDomain();
  }

  @Override
  public Optional<PlanDefinition> findById(UUID id) {
    return jpa.findById(id).map(PlanDefinitionEntity::toDomain);
  }

  @Override
  public Optional<PlanDefinition> findBySlug(String slug) {
    return jpa.findBySlug(slug).map(PlanDefinitionEntity::toDomain);
  }

  @Override
  public List<PlanDefinition> findAllActive() {
    return jpa.findByActiveTrue().stream().map(PlanDefinitionEntity::toDomain).toList();
  }
}
