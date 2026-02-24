package com.union.solutions.saascore.unit.application.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.union.solutions.saascore.adapters.out.persistence.OutboxEventEntity;
import com.union.solutions.saascore.adapters.out.persistence.OutboxEventJpaRepository;
import com.union.solutions.saascore.adapters.out.persistence.TenantEntity;
import com.union.solutions.saascore.adapters.out.persistence.TenantJpaRepository;
import com.union.solutions.saascore.application.abac.AuditLogger;
import com.union.solutions.saascore.application.tenant.TenantUseCase;
import com.union.solutions.saascore.domain.Tenant;
import io.micrometer.core.instrument.Counter;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantUseCaseTest {

  @Mock TenantJpaRepository tenantRepo;
  @Mock OutboxEventJpaRepository outboxRepo;
  @Mock AuditLogger auditLogger;
  @Mock Counter tenantsCreatedCounter;

  private TenantUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase =
        new TenantUseCase(
            tenantRepo, outboxRepo, auditLogger, new ObjectMapper(), tenantsCreatedCounter);
  }

  @Test
  void create_savesAndPublishesCreatedEvent() {
    when(tenantRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(outboxRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Tenant result = useCase.create("Acme", "pro", "us-east-1");

    assertThat(result.getName()).isEqualTo("Acme");
    assertThat(result.getPlan()).isEqualTo("pro");
    assertThat(result.getStatus()).isEqualTo(Tenant.TenantStatus.ACTIVE);
    verify(tenantsCreatedCounter).increment();

    ArgumentCaptor<OutboxEventEntity> outboxCaptor =
        ArgumentCaptor.forClass(OutboxEventEntity.class);
    verify(outboxRepo).save(outboxCaptor.capture());
    assertThat(outboxCaptor.getValue().getEventType()).isEqualTo("tenant.created");
  }

  @Test
  void softDelete_setsStatusAndPublishesDeletedEvent() {
    UUID id = UUID.randomUUID();
    TenantEntity entity = makeTenantEntity(id, "Delete Corp");
    when(tenantRepo.findById(id)).thenReturn(Optional.of(entity));
    when(tenantRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(outboxRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

    boolean result = useCase.softDelete(id);

    assertThat(result).isTrue();
    assertThat(entity.getStatus()).isEqualTo(Tenant.TenantStatus.DELETED);

    ArgumentCaptor<OutboxEventEntity> outboxCaptor =
        ArgumentCaptor.forClass(OutboxEventEntity.class);
    verify(outboxRepo).save(outboxCaptor.capture());
    assertThat(outboxCaptor.getValue().getEventType()).isEqualTo("tenant.deleted");
  }

  @Test
  void softDelete_notFound_returnsFalse() {
    UUID id = UUID.randomUUID();
    when(tenantRepo.findById(id)).thenReturn(Optional.empty());

    assertThat(useCase.softDelete(id)).isFalse();
  }

  @Test
  void update_modifiesFieldsAndPublishesEvent() {
    UUID id = UUID.randomUUID();
    TenantEntity entity = makeTenantEntity(id, "Old Name");
    when(tenantRepo.findById(id)).thenReturn(Optional.of(entity));
    when(tenantRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(outboxRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Optional<Tenant> result = useCase.update(id, "New Name", null, null, null);

    assertThat(result).isPresent();
    assertThat(result.get().getName()).isEqualTo("New Name");

    ArgumentCaptor<OutboxEventEntity> outboxCaptor =
        ArgumentCaptor.forClass(OutboxEventEntity.class);
    verify(outboxRepo).save(outboxCaptor.capture());
    assertThat(outboxCaptor.getValue().getEventType()).isEqualTo("tenant.updated");
  }

  @Test
  void getById_found_returnsTenant() {
    UUID id = UUID.randomUUID();
    TenantEntity entity = makeTenantEntity(id, "Found Corp");
    when(tenantRepo.findById(id)).thenReturn(Optional.of(entity));

    Optional<Tenant> result = useCase.getById(id);

    assertThat(result).isPresent();
    assertThat(result.get().getName()).isEqualTo("Found Corp");
  }

  @Test
  void getById_notFound_returnsEmpty() {
    UUID id = UUID.randomUUID();
    when(tenantRepo.findById(id)).thenReturn(Optional.empty());

    assertThat(useCase.getById(id)).isEmpty();
  }

  private TenantEntity makeTenantEntity(UUID id, String name) {
    Tenant t =
        new Tenant(
            id, name, "pro", "us-east-1", Tenant.TenantStatus.ACTIVE, Instant.now(), Instant.now());
    return TenantEntity.from(t);
  }
}
