package com.union.solutions.saascore.application.port;

import com.union.solutions.saascore.domain.PlanDefinition;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanDefinitionRepository {

  PlanDefinition save(PlanDefinition plan);

  Optional<PlanDefinition> findById(UUID id);

  Optional<PlanDefinition> findBySlug(String slug);

  List<PlanDefinition> findAllActive();
}
