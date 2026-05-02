package com.union.solutions.saascore.application.email;

import com.union.solutions.saascore.application.port.EmailSender;
import com.union.solutions.saascore.application.port.SubscriptionRepository;
import com.union.solutions.saascore.application.port.SubscriptionTrialReminderSentRepository;
import com.union.solutions.saascore.application.port.TenantRepository;
import com.union.solutions.saascore.application.port.UserRepository;
import com.union.solutions.saascore.domain.Subscription;
import com.union.solutions.saascore.domain.Tenant;
import com.union.solutions.saascore.domain.User;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Envia lembretes por e-mail 3 e 1 dia antes do fim do trial (calendário UTC), aos utilizadores
 * admin ativos do tenant. Idempotente por subscrição e tipo (TRIAL_ENDING_3D / TRIAL_ENDING_1D).
 */
@Component
public class TrialReminderEmailScheduler {

  private static final Logger log = LoggerFactory.getLogger(TrialReminderEmailScheduler.class);

  public static final String REMINDER_3D = "TRIAL_ENDING_3D";
  public static final String REMINDER_1D = "TRIAL_ENDING_1D";

  private static final DateTimeFormatter DATE_UTC =
      DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC);

  private final SubscriptionRepository subscriptionRepo;
  private final SubscriptionTrialReminderSentRepository reminderSentRepo;
  private final TenantRepository tenantRepo;
  private final UserRepository userRepo;
  private final EmailSender emailSender;
  private final String frontendUrl;
  private final boolean enabled;
  private final Clock clock;

  public TrialReminderEmailScheduler(
      SubscriptionRepository subscriptionRepo,
      SubscriptionTrialReminderSentRepository reminderSentRepo,
      TenantRepository tenantRepo,
      UserRepository userRepo,
      EmailSender emailSender,
      Clock clock,
      @Value("${app.email.frontend-url:http://localhost:4200}") String frontendUrl,
      @Value("${app.trial-reminder.enabled:true}") boolean enabled) {
    this.subscriptionRepo = subscriptionRepo;
    this.reminderSentRepo = reminderSentRepo;
    this.tenantRepo = tenantRepo;
    this.userRepo = userRepo;
    this.emailSender = emailSender;
    this.clock = clock;
    this.frontendUrl = frontendUrl.endsWith("/") ? frontendUrl : frontendUrl + "/";
    this.enabled = enabled;
  }

  @Scheduled(cron = "${app.trial-reminder.cron:0 0 8 * * *}")
  @Transactional
  public void sendTrialEndingReminders() {
    if (!enabled) {
      return;
    }
    LocalDate today = LocalDate.ofInstant(Instant.now(clock), ZoneOffset.UTC);
    sendForDaysBefore(today, 3, REMINDER_3D);
    sendForDaysBefore(today, 1, REMINDER_1D);
  }

  private void sendForDaysBefore(LocalDate todayUtc, int daysBefore, String reminderType) {
    LocalDate endDay = todayUtc.plusDays(daysBefore);
    Instant start = endDay.atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant end = endDay.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

    List<Subscription> trials = subscriptionRepo.findTrialsWithTrialEndingBetween(start, end);
    for (Subscription sub : trials) {
      if (reminderSentRepo.existsBySubscriptionIdAndReminderType(sub.getId(), reminderType)) {
        continue;
      }
      Tenant tenant =
          tenantRepo
              .findById(sub.getTenantId())
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "Tenant not found for trial reminder: " + sub.getTenantId()));

      List<User> admins =
          userRepo.findByTenantId(sub.getTenantId()).stream()
              .filter(u -> u.getStatus() == User.UserStatus.ACTIVE)
              .filter(u -> u.getRoles() != null && u.getRoles().contains("admin"))
              .toList();
      if (admins.isEmpty()) {
        log.warn(
            "Trial reminder skipped: no active admin for tenant={} subscription={}",
            sub.getTenantId(),
            sub.getId());
        continue;
      }

      String tenantName = tenant.getName() != null ? tenant.getName() : "sua organização";
      String endDateStr = DATE_UTC.format(sub.getTrialEndsAt());
      String billingUrl = frontendUrl + "billing";
      String subject =
          (daysBefore <= 1 ? "Último dia de trial" : "Trial termina em " + daysBefore + " dias")
              + " – "
              + EmailTemplates.PRODUCT_DISPLAY_NAME;

      boolean anyOk = false;
      for (User admin : admins) {
        try {
          emailSender.send(
              admin.getEmail(),
              subject,
              EmailTemplates.trialEndingReminderEmail(
                  admin.getName(), tenantName, endDateStr, billingUrl, daysBefore));
          anyOk = true;
          log.info(
              "Trial reminder {} sent subscription={} tenant={} to={}",
              reminderType,
              sub.getId(),
              sub.getTenantId(),
              admin.getEmail());
        } catch (Exception e) {
          log.warn(
              "Failed trial reminder {} for subscription={} user={}: {}",
              reminderType,
              sub.getId(),
              admin.getId(),
              e.getMessage());
        }
      }
      if (anyOk) {
        reminderSentRepo.recordSent(sub.getId(), reminderType);
      }
    }
  }
}
