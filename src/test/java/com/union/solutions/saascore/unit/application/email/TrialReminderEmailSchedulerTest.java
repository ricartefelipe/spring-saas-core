package com.union.solutions.saascore.unit.application.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.union.solutions.saascore.application.email.TrialReminderEmailScheduler;
import com.union.solutions.saascore.application.port.EmailSender;
import com.union.solutions.saascore.application.port.SubscriptionRepository;
import com.union.solutions.saascore.application.port.SubscriptionTrialReminderSentRepository;
import com.union.solutions.saascore.application.port.TenantRepository;
import com.union.solutions.saascore.application.port.UserRepository;
import com.union.solutions.saascore.domain.Subscription;
import com.union.solutions.saascore.domain.Subscription.SubscriptionStatus;
import com.union.solutions.saascore.domain.Tenant;
import com.union.solutions.saascore.domain.User;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
class TrialReminderEmailSchedulerTest {

  @Mock SubscriptionRepository subscriptionRepo;
  @Mock SubscriptionTrialReminderSentRepository reminderSentRepo;
  @Mock TenantRepository tenantRepo;
  @Mock UserRepository userRepo;
  @Mock EmailSender emailSender;

  private TrialReminderEmailScheduler scheduler;
  private UUID tenantId;
  private UUID subscriptionId;
  private Tenant tenant;
  private User adminUser;
  private Instant trialEndsAt;

  @BeforeEach
  void setUp() {
    tenantId = UUID.randomUUID();
    subscriptionId = UUID.randomUUID();
    tenant =
        new Tenant(
            tenantId,
            "Acme Corp",
            "starter",
            "us-east-1",
            Tenant.TenantStatus.ACTIVE,
            Instant.now(),
            Instant.now());
    adminUser =
        new User(
            UUID.randomUUID(),
            "admin@acme.com",
            "Admin",
            "hash",
            tenantId,
            List.of("admin"),
            User.UserStatus.ACTIVE,
            Instant.now(),
            Instant.now());
    // 2026-05-02 UTC + 3 days → trial ends 2026-05-05
    Clock clock = Clock.fixed(Instant.parse("2026-05-02T10:00:00Z"), ZoneOffset.UTC);
    trialEndsAt = LocalDate.of(2026, 5, 5).atTime(12, 0).toInstant(ZoneOffset.UTC);
    scheduler =
        new TrialReminderEmailScheduler(
            subscriptionRepo,
            reminderSentRepo,
            tenantRepo,
            userRepo,
            emailSender,
            clock,
            "https://admin.example.com",
            true);
  }

  @Test
  void sendTrialEndingReminders_sends3DayReminder_andRecords() {
    Subscription sub = trialSubscription();
    Instant wStart = LocalDate.of(2026, 5, 5).atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant wEnd = LocalDate.of(2026, 5, 6).atStartOfDay(ZoneOffset.UTC).toInstant();

    Instant d1Start = LocalDate.of(2026, 5, 3).atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant d1End = LocalDate.of(2026, 5, 4).atStartOfDay(ZoneOffset.UTC).toInstant();
    when(subscriptionRepo.findTrialsWithTrialEndingBetween(d1Start, d1End)).thenReturn(List.of());

    when(subscriptionRepo.findTrialsWithTrialEndingBetween(wStart, wEnd)).thenReturn(List.of(sub));
    when(reminderSentRepo.existsBySubscriptionIdAndReminderType(
            subscriptionId, TrialReminderEmailScheduler.REMINDER_3D))
        .thenReturn(false);
    when(tenantRepo.findById(tenantId)).thenReturn(Optional.of(tenant));
    when(userRepo.findByTenantId(tenantId)).thenReturn(List.of(adminUser));

    scheduler.sendTrialEndingReminders();

    ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
    verify(emailSender).send(eq("admin@acme.com"), subjectCaptor.capture(), anyString());
    assertThat(subjectCaptor.getValue()).contains("3 dias");
    verify(reminderSentRepo).recordSent(subscriptionId, TrialReminderEmailScheduler.REMINDER_3D);
  }

  @Test
  void sendTrialEndingReminders_skips_whenAlreadySent() {
    Subscription sub = trialSubscription();
    Instant wStart = LocalDate.of(2026, 5, 5).atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant wEnd = LocalDate.of(2026, 5, 6).atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant d1Start = LocalDate.of(2026, 5, 3).atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant d1End = LocalDate.of(2026, 5, 4).atStartOfDay(ZoneOffset.UTC).toInstant();
    when(subscriptionRepo.findTrialsWithTrialEndingBetween(d1Start, d1End)).thenReturn(List.of());

    when(subscriptionRepo.findTrialsWithTrialEndingBetween(wStart, wEnd)).thenReturn(List.of(sub));
    when(reminderSentRepo.existsBySubscriptionIdAndReminderType(
            subscriptionId, TrialReminderEmailScheduler.REMINDER_3D))
        .thenReturn(true);

    scheduler.sendTrialEndingReminders();

    verify(emailSender, never()).send(anyString(), anyString(), anyString());
    verify(reminderSentRepo, never()).recordSent(any(), anyString());
  }

  @Test
  void sendTrialEndingReminders_noop_whenDisabled() {
    Clock clock = Clock.fixed(Instant.parse("2026-05-02T10:00:00Z"), ZoneOffset.UTC);
    scheduler =
        new TrialReminderEmailScheduler(
            subscriptionRepo,
            reminderSentRepo,
            tenantRepo,
            userRepo,
            emailSender,
            clock,
            "https://admin.example.com",
            false);
    scheduler.sendTrialEndingReminders();
    verify(subscriptionRepo, never()).findTrialsWithTrialEndingBetween(any(), any());
  }

  private Subscription trialSubscription() {
    Instant now = Instant.parse("2026-04-21T10:00:00Z");
    return new Subscription(
        subscriptionId,
        tenantId,
        "starter",
        SubscriptionStatus.TRIAL,
        now,
        trialEndsAt,
        trialEndsAt,
        null,
        null,
        null,
        now,
        now);
  }
}
