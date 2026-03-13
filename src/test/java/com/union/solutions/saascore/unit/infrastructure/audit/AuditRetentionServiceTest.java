package com.union.solutions.saascore.unit.infrastructure.audit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.union.solutions.saascore.application.abac.AuditLogger;
import com.union.solutions.saascore.adapters.out.persistence.AuditLogJpaRepository;
import com.union.solutions.saascore.infrastructure.audit.AuditRetentionService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditRetentionServiceTest {

  @org.mockito.Mock AuditLogJpaRepository auditRepo;
  @org.mockito.Mock AuditLogger auditLogger;

  @Test
  void purge_deletesInBatches_untilNoneRemaining() {
    AuditRetentionService service = new AuditRetentionService(auditRepo, auditLogger, 90);

    when(auditRepo.deleteBatchOlderThan(any(Instant.class), eq(1000)))
        .thenReturn(1000)
        .thenReturn(1000)
        .thenReturn(200);

    service.purgeExpiredLogs();

    verify(auditRepo, times(3)).deleteBatchOlderThan(any(Instant.class), eq(1000));
  }

  @Test
  void purge_skipped_whenRetentionDaysIsZero() {
    AuditRetentionService service = new AuditRetentionService(auditRepo, auditLogger, 0);

    service.purgeExpiredLogs();

    verify(auditRepo, never()).deleteBatchOlderThan(any(Instant.class), eq(1000));
    verify(auditLogger, never()).log(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void purge_skipped_whenRetentionDaysIsNegative() {
    AuditRetentionService service = new AuditRetentionService(auditRepo, auditLogger, -1);

    service.purgeExpiredLogs();

    verify(auditRepo, never()).deleteBatchOlderThan(any(Instant.class), eq(1000));
    verify(auditLogger, never()).log(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void purge_singleBatch_whenFewerThanBatchSize() {
    AuditRetentionService service = new AuditRetentionService(auditRepo, auditLogger, 30);

    when(auditRepo.deleteBatchOlderThan(any(Instant.class), eq(1000))).thenReturn(500);

    service.purgeExpiredLogs();

    verify(auditRepo, times(1)).deleteBatchOlderThan(any(Instant.class), eq(1000));
  }

  @Test
  void purge_noop_whenNoExpiredRecords() {
    AuditRetentionService service = new AuditRetentionService(auditRepo, auditLogger, 90);

    when(auditRepo.deleteBatchOlderThan(any(Instant.class), eq(1000))).thenReturn(0);

    service.purgeExpiredLogs();

    verify(auditRepo, times(1)).deleteBatchOlderThan(any(Instant.class), eq(1000));
  }

  @Test
  void purge_logsAuditEntryWithCleanupAction() {
    AuditRetentionService service = new AuditRetentionService(auditRepo, auditLogger, 90);

    when(auditRepo.deleteBatchOlderThan(any(Instant.class), eq(1000))).thenReturn(150);

    service.purgeExpiredLogs();

    var detailsCaptor = ArgumentCaptor.forClass(String.class);
    verify(auditLogger)
        .log(
            isNull(),
            eq("system"),
            eq("[]"),
            eq("[]"),
            eq("audit.retention.cleanup"),
            eq("audit_log"),
            isNull(),
            isNull(),
            isNull(),
            eq(200),
            isNull(),
            detailsCaptor.capture());
    String details = detailsCaptor.getValue();
    assertTrue(details.contains("deleted=150"), "details must include deleted count");
    assertTrue(details.contains("retention_days=90"), "details must include retention_days");
  }
}
