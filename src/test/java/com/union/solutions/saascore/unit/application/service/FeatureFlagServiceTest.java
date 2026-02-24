package com.union.solutions.saascore.unit.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.union.solutions.saascore.adapters.out.persistence.FeatureFlagEntity;
import com.union.solutions.saascore.adapters.out.persistence.FeatureFlagJpaRepository;
import com.union.solutions.saascore.adapters.out.persistence.OutboxEventEntity;
import com.union.solutions.saascore.adapters.out.persistence.OutboxEventJpaRepository;
import com.union.solutions.saascore.application.abac.AuditLogger;
import com.union.solutions.saascore.application.service.FeatureFlagService;
import io.micrometer.core.instrument.Counter;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FeatureFlagServiceTest {

  @Mock FeatureFlagJpaRepository flagRepo;
  @Mock OutboxEventJpaRepository outboxRepo;
  @Mock AuditLogger auditLogger;
  @Mock Counter flagsToggledCounter;

  private FeatureFlagService service;

  @BeforeEach
  void setUp() {
    service =
        new FeatureFlagService(
            flagRepo, outboxRepo, auditLogger, new ObjectMapper(), flagsToggledCounter);
  }

  @Test
  void create_savesEntity() {
    UUID tenantId = UUID.randomUUID();
    when(flagRepo.findByTenantIdAndName(tenantId, "new_flag")).thenReturn(Optional.empty());
    when(flagRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(outboxRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

    FeatureFlagEntity result = service.create(tenantId, "new_flag", true, 50, List.of("admin"));

    assertThat(result.getName()).isEqualTo("new_flag");
    assertThat(result.isEnabled()).isTrue();
    assertThat(result.getRolloutPercent()).isEqualTo(50);
    verify(flagRepo).save(any());

    ArgumentCaptor<OutboxEventEntity> outboxCaptor =
        ArgumentCaptor.forClass(OutboxEventEntity.class);
    verify(outboxRepo).save(outboxCaptor.capture());
    assertThat(outboxCaptor.getValue().getEventType()).isEqualTo("flag.created");
  }

  @Test
  void create_duplicateName_throwsException() {
    UUID tenantId = UUID.randomUUID();
    FeatureFlagEntity existing = new FeatureFlagEntity();
    existing.setName("dup");
    when(flagRepo.findByTenantIdAndName(tenantId, "dup")).thenReturn(Optional.of(existing));

    assertThatThrownBy(() -> service.create(tenantId, "dup", true, 100, List.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("already exists");
  }

  @Test
  void create_clampsRolloutPercent() {
    UUID tenantId = UUID.randomUUID();
    when(flagRepo.findByTenantIdAndName(tenantId, "clamp")).thenReturn(Optional.empty());
    when(flagRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(outboxRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

    FeatureFlagEntity over = service.create(tenantId, "clamp", true, 150, List.of());
    assertThat(over.getRolloutPercent()).isEqualTo(100);
  }

  @Test
  void softDelete_setsDeletedFlag() {
    UUID tenantId = UUID.randomUUID();
    FeatureFlagEntity entity = makeFlag(tenantId, "del_flag");
    when(flagRepo.findByTenantIdAndName(tenantId, "del_flag")).thenReturn(Optional.of(entity));
    when(flagRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(outboxRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

    boolean result = service.softDelete(tenantId, "del_flag");

    assertThat(result).isTrue();
    assertThat(entity.isDeleted()).isTrue();
    assertThat(entity.getDeletedAt()).isNotNull();
  }

  @Test
  void softDelete_notFound_returnsFalse() {
    UUID tenantId = UUID.randomUUID();
    when(flagRepo.findByTenantIdAndName(tenantId, "missing")).thenReturn(Optional.empty());

    assertThat(service.softDelete(tenantId, "missing")).isFalse();
  }

  @Test
  void update_modifiesFields() {
    UUID tenantId = UUID.randomUUID();
    FeatureFlagEntity entity = makeFlag(tenantId, "upd_flag");
    when(flagRepo.findByTenantIdAndName(tenantId, "upd_flag")).thenReturn(Optional.of(entity));
    when(flagRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(outboxRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Optional<FeatureFlagEntity> result =
        service.update(tenantId, "upd_flag", false, 75, List.of("user"));

    assertThat(result).isPresent();
    assertThat(result.get().isEnabled()).isFalse();
    assertThat(result.get().getRolloutPercent()).isEqualTo(75);
    verify(flagsToggledCounter).increment();
  }

  private FeatureFlagEntity makeFlag(UUID tenantId, String name) {
    FeatureFlagEntity e = new FeatureFlagEntity();
    e.setId(UUID.randomUUID());
    e.setTenantId(tenantId);
    e.setName(name);
    e.setEnabled(true);
    e.setRolloutPercent(100);
    e.setAllowedRoles("[]");
    Instant now = Instant.now();
    e.setCreatedAt(now);
    e.setUpdatedAt(now);
    return e;
  }
}
