package com.union.solutions.saascore.unit.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.union.solutions.saascore.application.abac.AuditLogger;
import com.union.solutions.saascore.application.port.FeatureFlagRepository;
import com.union.solutions.saascore.application.port.OutboxPublisherPort;
import com.union.solutions.saascore.application.service.FeatureFlagService;
import com.union.solutions.saascore.domain.FeatureFlag;
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
class FeatureFlagServiceTest {

  @Mock FeatureFlagRepository flagRepo;
  @Mock OutboxPublisherPort outboxPublisher;
  @Mock AuditLogger auditLogger;
  @Mock Counter flagsToggledCounter;

  private FeatureFlagService service;

  @BeforeEach
  void setUp() {
    service = new FeatureFlagService(flagRepo, outboxPublisher, auditLogger, flagsToggledCounter);
  }

  @Test
  void create_savesEntity() {
    UUID tenantId = UUID.randomUUID();
    when(flagRepo.createOrResurrect(eq(tenantId), eq("new_flag"), eq(true), eq(50), eq(List.of("admin"))))
        .thenAnswer(inv -> new FeatureFlag(
            UUID.randomUUID(), inv.getArgument(0), inv.getArgument(1),
            inv.getArgument(2), inv.getArgument(3), inv.getArgument(4),
            Instant.now(), Instant.now()));

    FeatureFlag result = service.create(tenantId, "new_flag", true, 50, List.of("admin"));

    assertThat(result.getName()).isEqualTo("new_flag");
    assertThat(result.isEnabled()).isTrue();
    assertThat(result.getRolloutPercent()).isEqualTo(50);
    verify(flagRepo).createOrResurrect(eq(tenantId), eq("new_flag"), eq(true), eq(50), eq(List.of("admin")));
    verify(outboxPublisher).publish(eq("FLAG"), anyString(), eq("flag.created"), anyMap());
  }

  @Test
  void create_duplicateName_throwsException() {
    UUID tenantId = UUID.randomUUID();
    when(flagRepo.createOrResurrect(eq(tenantId), eq("dup"), anyBoolean(), anyInt(), anyList()))
        .thenThrow(new IllegalArgumentException("Flag already exists for tenant"));

    assertThatThrownBy(() -> service.create(tenantId, "dup", true, 100, List.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("already exists");
  }

  @Test
  void create_clampsRolloutPercent() {
    UUID tenantId = UUID.randomUUID();
    when(flagRepo.createOrResurrect(eq(tenantId), eq("clamp"), eq(true), eq(150), eq(List.of())))
        .thenAnswer(inv -> new FeatureFlag(
            UUID.randomUUID(), inv.getArgument(0), inv.getArgument(1),
            inv.getArgument(2), inv.getArgument(3), inv.getArgument(4),
            Instant.now(), Instant.now()));

    FeatureFlag over = service.create(tenantId, "clamp", true, 150, List.of());
    assertThat(over.getRolloutPercent()).isEqualTo(100);
  }

  @Test
  void softDelete_setsDeletedFlag() {
    UUID tenantId = UUID.randomUUID();
    FeatureFlag flag = makeFlag(tenantId, "del_flag");
    when(flagRepo.findByTenantIdAndName(tenantId, "del_flag")).thenReturn(Optional.of(flag));
    when(flagRepo.softDelete(tenantId, "del_flag")).thenReturn(true);

    boolean result = service.softDelete(tenantId, "del_flag");

    assertThat(result).isTrue();
    verify(flagRepo).softDelete(tenantId, "del_flag");
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
    FeatureFlag flag = makeFlag(tenantId, "upd_flag");
    when(flagRepo.findByTenantIdAndName(tenantId, "upd_flag")).thenReturn(Optional.of(flag));
    when(flagRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Optional<FeatureFlag> result =
        service.update(tenantId, "upd_flag", false, 75, List.of("user"));

    assertThat(result).isPresent();
    assertThat(result.get().isEnabled()).isFalse();
    assertThat(result.get().getRolloutPercent()).isEqualTo(75);
    verify(flagsToggledCounter).increment();
  }

  private FeatureFlag makeFlag(UUID tenantId, String name) {
    return new FeatureFlag(
        UUID.randomUUID(),
        tenantId,
        name,
        true,
        100,
        List.of(),
        Instant.now(),
        Instant.now());
  }
}
