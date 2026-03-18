package com.union.solutions.saascore.unit.application.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.union.solutions.saascore.application.email.ReactivationEmailScheduler;
import com.union.solutions.saascore.application.port.EmailSender;
import com.union.solutions.saascore.application.port.ReactivationSentRepository;
import com.union.solutions.saascore.application.port.TenantRepository;
import com.union.solutions.saascore.application.port.UserRepository;
import com.union.solutions.saascore.domain.Tenant;
import com.union.solutions.saascore.domain.User;
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
class ReactivationEmailSchedulerTest {

  @Mock TenantRepository tenantRepo;
  @Mock UserRepository userRepo;
  @Mock EmailSender emailSender;
  @Mock ReactivationSentRepository reactivationSentRepo;

  private ReactivationEmailScheduler scheduler;
  private UUID tenantId;
  private User adminUser;
  private Tenant tenant;
  private static final String FRONTEND_URL = "https://app.example.com/";
  private static final int INACTIVE_DAYS = 14;
  private static final int COOLDOWN_DAYS = 30;

  @BeforeEach
  void setUp() {
    scheduler =
        new ReactivationEmailScheduler(
            tenantRepo,
            userRepo,
            emailSender,
            reactivationSentRepo,
            FRONTEND_URL,
            INACTIVE_DAYS,
            COOLDOWN_DAYS);
    tenantId = UUID.randomUUID();
    tenant =
        new Tenant(
            tenantId,
            "Acme Corp",
            "pro",
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
  }

  @Test
  void sendReactivationEmails_sendsToAdminAndRecords_whenTenantInactiveAndNotInCooldown() {
    when(tenantRepo.findAllActiveIds()).thenReturn(List.of(tenantId));
    Instant oldLogin = Instant.now().minus(20, java.time.temporal.ChronoUnit.DAYS);
    when(userRepo.findMaxLastLoginAtByTenantId(tenantId)).thenReturn(Optional.of(oldLogin));
    when(reactivationSentRepo.wasSentAfter(eq(tenantId), any(Instant.class))).thenReturn(false);
    when(tenantRepo.findById(tenantId)).thenReturn(Optional.of(tenant));
    when(userRepo.findByTenantId(tenantId)).thenReturn(List.of(adminUser));

    scheduler.sendReactivationEmails();

    ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
    verify(emailSender).send(eq("admin@acme.com"), subjectCaptor.capture(), bodyCaptor.capture());
    verify(reactivationSentRepo).record(eq(tenantId), any(Instant.class));
    assertThat(subjectCaptor.getValue()).contains("Reative sua conta");
    assertThat(bodyCaptor.getValue())
        .contains("Admin")
        .contains("Acme Corp")
        .contains(FRONTEND_URL + "login");
  }

  @Test
  void sendReactivationEmails_skipsTenant_whenRecentlyActive() {
    when(tenantRepo.findAllActiveIds()).thenReturn(List.of(tenantId));
    Instant recentLogin = Instant.now().minus(2, java.time.temporal.ChronoUnit.DAYS);
    when(userRepo.findMaxLastLoginAtByTenantId(tenantId)).thenReturn(Optional.of(recentLogin));

    scheduler.sendReactivationEmails();

    verify(emailSender, never()).send(any(), any(), any());
    verify(reactivationSentRepo, never()).record(any(), any());
  }

  @Test
  void sendReactivationEmails_skipsTenant_whenInCooldown() {
    when(tenantRepo.findAllActiveIds()).thenReturn(List.of(tenantId));
    Instant oldLogin = Instant.now().minus(20, java.time.temporal.ChronoUnit.DAYS);
    when(userRepo.findMaxLastLoginAtByTenantId(tenantId)).thenReturn(Optional.of(oldLogin));
    when(reactivationSentRepo.wasSentAfter(eq(tenantId), any(Instant.class))).thenReturn(true);

    scheduler.sendReactivationEmails();

    verify(emailSender, never()).send(any(), any(), any());
    verify(reactivationSentRepo, never()).record(any(), any());
  }

  @Test
  void sendReactivationEmails_sendsToFirstUser_whenNoAdmin() {
    User regularUser =
        new User(
            UUID.randomUUID(),
            "user@acme.com",
            "User",
            "hash",
            tenantId,
            List.of("user"),
            User.UserStatus.ACTIVE,
            Instant.now(),
            Instant.now());
    when(tenantRepo.findAllActiveIds()).thenReturn(List.of(tenantId));
    Instant oldLogin = Instant.now().minus(20, java.time.temporal.ChronoUnit.DAYS);
    when(userRepo.findMaxLastLoginAtByTenantId(tenantId)).thenReturn(Optional.of(oldLogin));
    when(reactivationSentRepo.wasSentAfter(eq(tenantId), any(Instant.class))).thenReturn(false);
    when(tenantRepo.findById(tenantId)).thenReturn(Optional.of(tenant));
    when(userRepo.findByTenantId(tenantId)).thenReturn(List.of(regularUser));

    scheduler.sendReactivationEmails();

    verify(emailSender).send(eq("user@acme.com"), any(), any());
    verify(reactivationSentRepo).record(eq(tenantId), any(Instant.class));
  }
}
