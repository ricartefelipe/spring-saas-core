package com.union.solutions.saascore.application.port;

import com.union.solutions.saascore.domain.Policy;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Port de persistência de políticas ABAC. A aplicação depende apenas desta interface. */
public interface PolicyRepository {

  Policy save(Policy policy);

  Optional<Policy> findActiveById(UUID id);

  Page<Policy> search(String permissionCode, Policy.Effect effect, Boolean enabled, Pageable pageable);

  List<Policy> findByEnabledTrue();

  List<Policy> findByPermissionCodeAndEnabledTrue(String permissionCode);

  long countActive();

  boolean softDelete(UUID id);
}
