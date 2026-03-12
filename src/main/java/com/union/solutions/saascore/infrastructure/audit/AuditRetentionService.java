package com.union.solutions.saascore.infrastructure.audit;

import com.union.solutions.saascore.application.abac.AuditLogger;
import com.union.solutions.saascore.adapters.out.persistence.AuditLogJpaRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AuditRetentionService {

  private static final Logger log = LoggerFactory.getLogger(AuditRetentionService.class);
  private static final int BATCH_SIZE = 1000;
  private static final String ACTION_CLEANUP = "audit.retention.cleanup";

  private final AuditLogJpaRepository auditRepo;
  private final AuditLogger auditLogger;
  private final int retentionDays;

  public AuditRetentionService(
      AuditLogJpaRepository auditRepo,
      AuditLogger auditLogger,
      @Value("${app.audit.retention-days:90}") int retentionDays) {
    this.auditRepo = auditRepo;
    this.auditLogger = auditLogger;
    this.retentionDays = retentionDays;
  }

  @Scheduled(cron = "0 0 2 * * *")
  public void purgeExpiredLogs() {
    if (retentionDays <= 0) {
      log.info("Audit retention disabled (retention-days={}), skipping purge", retentionDays);
      return;
    }

    Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
    log.info(
        "Starting audit log retention purge cutoff={} retention_days={}", cutoff, retentionDays);

    int totalDeleted = 0;
    int deleted;
    do {
      deleted = deleteBatch(cutoff);
      totalDeleted += deleted;
    } while (deleted >= BATCH_SIZE);

    log.info(
        "Audit log retention purge completed total_deleted={} cutoff={}", totalDeleted, cutoff);

    String details = "deleted=" + totalDeleted + ",cutoff=" + cutoff + ",retention_days=" + retentionDays;
    auditLogger.log(
        null,
        "system",
        "[]",
        "[]",
        ACTION_CLEANUP,
        "audit_log",
        null,
        null,
        null,
        200,
        null,
        details);
  }

  @Transactional
  protected int deleteBatch(Instant cutoff) {
    return auditRepo.deleteBatchOlderThan(cutoff, BATCH_SIZE);
  }
}
