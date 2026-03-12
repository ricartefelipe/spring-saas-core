package com.union.solutions.saascore.adapters.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanDefinitionJpaRepository extends JpaRepository<PlanDefinitionEntity, UUID> {

  Optional<PlanDefinitionEntity> findBySlug(String slug);

  List<PlanDefinitionEntity> findByActiveTrue();
}
