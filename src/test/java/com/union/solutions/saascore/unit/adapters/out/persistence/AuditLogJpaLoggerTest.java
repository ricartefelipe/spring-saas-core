package com.union.solutions.saascore.unit.adapters.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.union.solutions.saascore.adapters.out.persistence.AuditLogEntity;
import com.union.solutions.saascore.adapters.out.persistence.AuditLogJpaLogger;
import com.union.solutions.saascore.adapters.out.persistence.AuditLogJpaRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditLogJpaLoggerTest {

  @Mock AuditLogJpaRepository repo;

  AuditLogJpaLogger logger;

  @BeforeEach
  void setUp() {
    logger = new AuditLogJpaLogger(repo);
  }

  @Test
  void log_truncatesActorSubToFitVarchar255() {
    String longSub = "s".repeat(300);
    UUID tenantId = UUID.randomUUID();
    UUID resource = UUID.randomUUID();

    logger.log(
        tenantId,
        longSub,
        "[]",
        "[]",
        "USER_DELETED",
        "user",
        resource.toString(),
        "DELETE",
        "/v1/users/" + resource,
        204,
        "correlation-id",
        null);

    ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
    verify(repo).save(captor.capture());
    assertThat(captor.getValue().getActorSub()).hasSize(255);
  }

  @Test
  void log_truncatesCorrelationIdToFitVarchar64() {
    String longCorr = "c".repeat(100);
    UUID tenantId = UUID.randomUUID();

    logger.log(
        tenantId, "subj@example.com", "[]", "[]", "X", "t", "id", "GET", "/p", 200, longCorr, null);

    ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
    verify(repo).save(captor.capture());
    assertThat(captor.getValue().getCorrelationId()).hasSize(64);
  }
}
