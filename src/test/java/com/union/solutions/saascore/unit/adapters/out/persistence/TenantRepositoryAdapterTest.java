package com.union.solutions.saascore.unit.adapters.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

import com.union.solutions.saascore.adapters.out.persistence.TenantJpaRepository;
import com.union.solutions.saascore.adapters.out.persistence.TenantRepositoryAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantRepositoryAdapterTest {

  @Mock TenantJpaRepository jpa;

  @Test
  void findById_returnsEmptyAndDoesNotCallJpaWhenIdIsNull() {
    TenantRepositoryAdapter adapter = new TenantRepositoryAdapter(jpa);

    assertThat(adapter.findById(null)).isEmpty();
    verifyNoInteractions(jpa);
  }
}
