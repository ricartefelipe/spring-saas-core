package com.union.solutions.saascore.application.service;

import com.union.solutions.saascore.adapters.out.persistence.AuditLogJpaRepository;
import com.union.solutions.saascore.adapters.out.persistence.FeatureFlagJpaRepository;
import com.union.solutions.saascore.adapters.out.persistence.PolicyJpaRepository;
import com.union.solutions.saascore.adapters.out.persistence.TenantJpaRepository;
import com.union.solutions.saascore.domain.Policy;
import com.union.solutions.saascore.domain.Tenant;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsService {

  private final TenantJpaRepository tenantRepo;
  private final PolicyJpaRepository policyRepo;
  private final FeatureFlagJpaRepository flagRepo;
  private final AuditLogJpaRepository auditRepo;

  @Value("${analytics.anomaly.burst-threshold:50}")
  private int burstThreshold;

  @Value("${analytics.anomaly.denied-threshold:10}")
  private int deniedThreshold;

  @Value("${analytics.anomaly.tenant-switch-threshold:5}")
  private int tenantSwitchThreshold;

  @Value("${analytics.anomaly.off-hours-start:0}")
  private int offHoursStart;

  @Value("${analytics.anomaly.off-hours-end:5}")
  private int offHoursEnd;

  public AnalyticsService(
      TenantJpaRepository tenantRepo,
      PolicyJpaRepository policyRepo,
      FeatureFlagJpaRepository flagRepo,
      AuditLogJpaRepository auditRepo) {
    this.tenantRepo = tenantRepo;
    this.policyRepo = policyRepo;
    this.flagRepo = flagRepo;
    this.auditRepo = auditRepo;
  }

  @Transactional(readOnly = true)
  public SummaryResponse getSummary() {
    Map<String, Long> byPlan = new LinkedHashMap<>();
    for (Object[] row : tenantRepo.countGroupByPlan()) {
      byPlan.put((String) row[0], (Long) row[1]);
    }

    Map<String, Long> byStatus = new LinkedHashMap<>();
    for (Object[] row : tenantRepo.countGroupByStatus()) {
      byStatus.put(((Tenant.TenantStatus) row[0]).name(), (Long) row[1]);
    }

    Map<String, Long> byRegion = new LinkedHashMap<>();
    for (Object[] row : tenantRepo.countGroupByRegion()) {
      byRegion.put((String) row[0], (Long) row[1]);
    }

    long tenantTotal = byStatus.values().stream().mapToLong(Long::longValue).sum();
    TenantSummary tenants = new TenantSummary(tenantTotal, byPlan, byStatus, byRegion);

    Map<String, Long> byEffect = new LinkedHashMap<>();
    for (Object[] row : policyRepo.countActiveGroupByEffect()) {
      byEffect.put(((Policy.Effect) row[0]).name(), (Long) row[1]);
    }
    long policyTotal = policyRepo.countActive();
    PolicySummary policies = new PolicySummary(policyTotal, byEffect);

    long flagTotal = flagRepo.countTotalNonDeleted();
    long flagEnabled = flagRepo.countActiveFlags();
    long flagDisabled = flagRepo.countDisabledFlags();
    FlagSummary flags = new FlagSummary(flagTotal, flagEnabled, flagDisabled);

    Instant now = Instant.now();
    long last24h = auditRepo.countSince(now.minus(24, ChronoUnit.HOURS));
    long last7d = auditRepo.countSince(now.minus(7, ChronoUnit.DAYS));

    List<ActionCount> topActions = new ArrayList<>();
    for (Object[] row : auditRepo.topActionsSince(now.minus(7, ChronoUnit.DAYS), 10)) {
      topActions.add(new ActionCount((String) row[0], ((Number) row[1]).longValue()));
    }
    AuditSummary audit = new AuditSummary(last24h, last7d, topActions);

    return new SummaryResponse(tenants, policies, flags, audit);
  }

  @Transactional(readOnly = true)
  public AnomalyResponse detectAnomalies() {
    Instant now = Instant.now();
    Instant since = now.minus(24, ChronoUnit.HOURS);
    List<Anomaly> anomalies = new ArrayList<>();

    for (Object[] row : auditRepo.findBurstAccess(since, burstThreshold)) {
      anomalies.add(
          new Anomaly(
              "burst_access",
              "high",
              (String) row[0],
              (String) row[1],
              ((Number) row[2]).longValue(),
              "5m",
              toInstant(row[4])));
    }

    for (Object[] row : auditRepo.findAccessDeniedSpikes(since, deniedThreshold)) {
      anomalies.add(
          new Anomaly(
              "access_denied_spike",
              "high",
              (String) row[0],
              null,
              ((Number) row[1]).longValue(),
              "1h",
              toInstant(row[3])));
    }

    for (Object[] row : auditRepo.findOffHoursActivity(since, offHoursStart, offHoursEnd)) {
      anomalies.add(
          new Anomaly(
              "off_hours_activity",
              "medium",
              (String) row[0],
              (String) row[1],
              ((Number) row[2]).longValue(),
              offHoursStart + ":00-" + offHoursEnd + ":00 UTC",
              toInstant(row[4])));
    }

    for (Object[] row : auditRepo.findUnusualTenantSwitching(since, tenantSwitchThreshold)) {
      anomalies.add(
          new Anomaly(
              "unusual_tenant_switching",
              "medium",
              (String) row[0],
              null,
              ((Number) row[1]).longValue(),
              "1h",
              toInstant(row[3])));
    }

    long totalEvents = auditRepo.countSince(since);
    return new AnomalyResponse(anomalies, "last 24 hours", totalEvents);
  }

  private static Instant toInstant(Object value) {
    if (value instanceof Instant i) return i;
    if (value instanceof Timestamp ts) return ts.toInstant();
    return Instant.now();
  }

  public record SummaryResponse(
      TenantSummary tenants, PolicySummary policies, FlagSummary flags, AuditSummary audit) {}

  public record TenantSummary(
      long total,
      Map<String, Long> byPlan,
      Map<String, Long> byStatus,
      Map<String, Long> byRegion) {}

  public record PolicySummary(long total, Map<String, Long> byEffect) {}

  public record FlagSummary(long total, long enabled, long disabled) {}

  public record AuditSummary(long last24h, long last7d, List<ActionCount> topActions) {}

  public record ActionCount(String action, long count) {}

  public record AnomalyResponse(List<Anomaly> anomalies, String scannedPeriod, long totalEvents) {}

  public record Anomaly(
      String type,
      String severity,
      String actor,
      String tenant,
      long count,
      String window,
      Instant detectedAt) {}
}
