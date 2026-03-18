package com.union.solutions.saascore.unit.application.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.union.solutions.saascore.application.email.PostSignupEmailScheduler;
import com.union.solutions.saascore.application.port.EmailSender;
import com.union.solutions.saascore.application.port.PostSignupSentRepository;
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
class PostSignupEmailSchedulerTest {

  @Mock UserRepository userRepo;
  @Mock TenantRepository tenantRepo;
  @Mock EmailSender emailSender;
  @Mock PostSignupSentRepository sentRepo;

  private PostSignupEmailScheduler scheduler;
  private UUID tenantId;
  private UUID userId;
  private User user;
  private Instant now;

  @BeforeEach
  void setUp() {
    scheduler = new PostSignupEmailScheduler(userRepo, tenantRepo, emailSender, sentRepo);
    tenantId = UUID.randomUUID();
    userId = UUID.randomUUID();
    now = Instant.now();
    user =
        new User(
            userId,
            "user@acme.com",
            "Maria",
            "hash",
            tenantId,
            List.of("user"),
            User.UserStatus.ACTIVE,
            now.minus(3, java.time.temporal.ChronoUnit.DAYS),
            now);
  }

  @Test
  void sendScheduledPostSignupEmails_sendsDay3AndRecords_whenUserInWindowAndNotSent() {
    when(userRepo.findByCreatedAtBetween(any(Instant.class), any(Instant.class)))
        .thenReturn(List.of(user))
        .thenReturn(List.of());
    when(sentRepo.existsByUserIdAndEmailType(userId, "DAY3")).thenReturn(false);
    when(tenantRepo.findById(tenantId))
        .thenReturn(Optional.of(new Tenant(tenantId, "Acme", "pro", "us-east-1", Tenant.TenantStatus.ACTIVE, now, now)));

    scheduler.sendScheduledPostSignupEmails();

    ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
    verify(emailSender).send(eq("user@acme.com"), subjectCaptor.capture(), bodyCaptor.capture());
    verify(sentRepo).recordSent(userId, "DAY3");
    assertThat(subjectCaptor.getValue()).contains("Dica de uso");
    assertThat(bodyCaptor.getValue()).contains("Maria").contains("Acme");
  }

  @Test
  void sendScheduledPostSignupEmails_skipsUser_whenDay3AlreadySent() {
    when(userRepo.findByCreatedAtBetween(any(Instant.class), any(Instant.class)))
        .thenReturn(List.of(user))
        .thenReturn(List.of());
    when(sentRepo.existsByUserIdAndEmailType(userId, "DAY3")).thenReturn(true);

    scheduler.sendScheduledPostSignupEmails();

    verify(emailSender, never()).send(any(), any(), any());
    verify(sentRepo, never()).recordSent(any(), any());
  }

  @Test
  void sendScheduledPostSignupEmails_usesDefaultTenantName_whenTenantNotFound() {
    when(userRepo.findByCreatedAtBetween(any(Instant.class), any(Instant.class)))
        .thenReturn(List.of(user))
        .thenReturn(List.of());
    when(sentRepo.existsByUserIdAndEmailType(userId, "DAY3")).thenReturn(false);
    when(tenantRepo.findById(tenantId)).thenReturn(Optional.empty());

    scheduler.sendScheduledPostSignupEmails();

    ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
    verify(emailSender).send(eq("user@acme.com"), any(), bodyCaptor.capture());
    assertThat(bodyCaptor.getValue()).contains("sua organização");
    verify(sentRepo).recordSent(userId, "DAY3");
  }
}
