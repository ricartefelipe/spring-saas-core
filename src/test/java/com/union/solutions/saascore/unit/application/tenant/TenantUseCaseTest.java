package com.union.solutions.saascore.unit.application.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.union.solutions.saascore.adapters.out.persistence.TenantEntity;
import com.union.solutions.saascore.adapters.out.persistence.TenantJpaRepository;
import com.union.solutions.saascore.application.abac.AuditLogger;
import com.union.solutions.saascore.application.port.OutboxPublisherPort;
import com.union.solutions.saascore.application.tenant.TenantUseCase;
import com.union.solutions.saascore.domain.Tenant;
import io.micrometer.core.instrument.Counter;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantUseCaseTest {

  @Mock TenantJpaRepository tenantRepo;
  @Mock OutboxPublisherPort outboxPublisher;
  @Mock AuditLogger auditLogger;
  @Mock Counter tenantsCreatedCounter;

  private TenantUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase = new TenantUseCase(tenantRepo, outboxPublisher, auditLogger, tenantsCreatedCounter);
  }

  @Test
  void create_savesAndPublishesCreatedEvent() {
    when(tenantRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Tenant result = useCase.create("Acme", "pro", "us-east-1");

    assertThat(result.getName()).isEqualTo("Acme");
    assertThat(result.getPlan()).isEqualTo("pro");
    assertThat(result.getStatus()).isEqualTo(Tenant.TenantStatus.ACTIVE);
    verify(tenantsCreatedCounter).increment();
    verify(outboxPublisher).publish(eq("TENANT"), anyString(), eq("tenant.created"), anyMap());
  }

  @Test
  void softDelete_setsStatusAndPublishesDeletedEvent() {
    UUID id = UUID.randomUUID();
    TenantEntity entity = makeTenantEntity(id, "Delete Corp");
    when(tenantRepo.findById(id)).thenReturn(Optional.of(entity));
    when(tenantRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

    boolean result = useCase.softDelete(id);

    assertThat(result).isTrue();
    assertThat(entity.getStatus()).isEqualTo(Tenant.TenantStatus.DELETED);
    verify(outboxPublisher)
        .publish(eq("TENANT"), eq(id.toString()), eq("tenant.deleted"), anyMap());
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

    Optional<Tenant> result = useCase.update(id, "New Name", null, null, null);

    assertThat(result).isPresent();
    assertThat(result.get().getName()).isEqualTo("New Name");
    verify(outboxPublisher)
        .publish(eq("TENANT"), eq(id.toString()), eq("tenant.updated"), anyMap());
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
