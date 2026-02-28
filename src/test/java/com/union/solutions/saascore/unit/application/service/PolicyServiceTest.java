package com.union.solutions.saascore.unit.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.union.solutions.saascore.adapters.out.persistence.PolicyEntity;
import com.union.solutions.saascore.adapters.out.persistence.PolicyJpaRepository;
import com.union.solutions.saascore.application.abac.AuditLogger;
import com.union.solutions.saascore.application.port.OutboxPublisherPort;
import com.union.solutions.saascore.application.service.PolicyService;
import com.union.solutions.saascore.domain.Policy;
import io.micrometer.core.instrument.Counter;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

  @Mock PolicyJpaRepository policyRepo;
  @Mock OutboxPublisherPort outboxPublisher;
  @Mock AuditLogger auditLogger;
  @Mock Counter policiesUpdatedCounter;

  private PolicyService service;

  @BeforeEach
  void setUp() {
    service =
        new PolicyService(
            policyRepo, outboxPublisher, auditLogger, new ObjectMapper(), policiesUpdatedCounter);
  }

  @Test
  void create_savesEntityAndPublishesOutbox() {
    when(policyRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

    PolicyEntity result =
        service.create("test:read", Policy.Effect.ALLOW, List.of("pro"), List.of(), true, "note");

    assertThat(result.getPermissionCode()).isEqualTo("test:read");
    assertThat(result.getEffect()).isEqualTo(Policy.Effect.ALLOW);
    assertThat(result.isEnabled()).isTrue();

    verify(policyRepo).save(any(PolicyEntity.class));
    verify(outboxPublisher).publish(eq("POLICY"), anyString(), eq("policy.created"), anyMap());
  }

  @Test
  void softDelete_setsDeletedFlag() {
    UUID id = UUID.randomUUID();
    PolicyEntity entity = makePolicyEntity(id, "test:delete");
    when(policyRepo.findActiveById(id)).thenReturn(Optional.of(entity));
    when(policyRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

    boolean result = service.softDelete(id);

    assertThat(result).isTrue();
    assertThat(entity.isDeleted()).isTrue();
    assertThat(entity.getDeletedAt()).isNotNull();
    verify(outboxPublisher)
        .publish(eq("POLICY"), eq(id.toString()), eq("policy.deleted"), anyMap());
  }

  @Test
  void softDelete_notFound_returnsFalse() {
    UUID id = UUID.randomUUID();
    when(policyRepo.findActiveById(id)).thenReturn(Optional.empty());

    boolean result = service.softDelete(id);

    assertThat(result).isFalse();
    verify(policyRepo, never()).save(any());
  }

  @Test
  void update_modifiesFields() {
    UUID id = UUID.randomUUID();
    PolicyEntity entity = makePolicyEntity(id, "test:update");
    when(policyRepo.findActiveById(id)).thenReturn(Optional.of(entity));
    when(policyRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Optional<PolicyEntity> result =
        service.update(id, null, Policy.Effect.DENY, null, null, false, "updated");

    assertThat(result).isPresent();
    assertThat(result.get().getEffect()).isEqualTo(Policy.Effect.DENY);
    assertThat(result.get().isEnabled()).isFalse();
    assertThat(result.get().getNotes()).isEqualTo("updated");
    verify(policiesUpdatedCounter).increment();
  }

  @Test
  void getApplicablePolicies_filtersCorrectly() {
    PolicyEntity p1 = makePolicyEntity(UUID.randomUUID(), "test:a");
    p1.setAllowedPlans("[\"pro\"]");
    p1.setAllowedRegions("[]");

    PolicyEntity p2 = makePolicyEntity(UUID.randomUUID(), "test:b");
    p2.setAllowedPlans("[]");
    p2.setAllowedRegions("[\"eu-west-1\"]");

    when(policyRepo.findByEnabledTrue()).thenReturn(List.of(p1, p2));

    List<PolicyEntity> result = service.getApplicablePolicies("pro", "us-east-1");
    assertThat(result).hasSize(1);
    assertThat(result.getFirst().getPermissionCode()).isEqualTo("test:a");
  }

  private PolicyEntity makePolicyEntity(UUID id, String permCode) {
    PolicyEntity e = new PolicyEntity();
    e.setId(id);
    e.setPermissionCode(permCode);
    e.setEffect(Policy.Effect.ALLOW);
    e.setAllowedPlans("[]");
    e.setAllowedRegions("[]");
    e.setEnabled(true);
    e.setNotes("test");
    Instant now = Instant.now();
    e.setCreatedAt(now);
    e.setUpdatedAt(now);
    return e;
  }
}
