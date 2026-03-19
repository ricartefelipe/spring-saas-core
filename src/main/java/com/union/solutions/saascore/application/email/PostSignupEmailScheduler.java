package com.union.solutions.saascore.application.email;

import com.union.solutions.saascore.application.port.EmailSender;
import com.union.solutions.saascore.application.port.PostSignupSentRepository;
import com.union.solutions.saascore.application.port.TenantRepository;
import com.union.solutions.saascore.application.port.UserRepository;
import com.union.solutions.saascore.domain.User;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sends post-signup emails on day 3 and day 7 after user creation. Day 1 (welcome) is sent
 * immediately by {@link com.union.solutions.saascore.application.onboarding.OnboardingUseCase}.
 */
@Component
public class PostSignupEmailScheduler {

  private static final Logger log = LoggerFactory.getLogger(PostSignupEmailScheduler.class);
  private static final String TYPE_DAY3 = "DAY3";
  private static final String TYPE_DAY7 = "DAY7";

  private final UserRepository userRepo;
  private final TenantRepository tenantRepo;
  private final EmailSender emailSender;
  private final PostSignupSentRepository sentRepo;

  public PostSignupEmailScheduler(
      UserRepository userRepo,
      TenantRepository tenantRepo,
      EmailSender emailSender,
      PostSignupSentRepository sentRepo) {
    this.userRepo = userRepo;
    this.tenantRepo = tenantRepo;
    this.emailSender = emailSender;
    this.sentRepo = sentRepo;
  }

  @Scheduled(cron = "${app.post-signup-email.cron:0 0 9 * * *}")
  @Transactional
  public void sendScheduledPostSignupEmails() {
    Instant now = Instant.now();
    sendForWindow(
        now.minus(3, ChronoUnit.DAYS).minus(12, ChronoUnit.HOURS),
        now.minus(3, ChronoUnit.DAYS).plus(12, ChronoUnit.HOURS),
        TYPE_DAY3,
        "Dica de uso",
        EmailTemplates::postSignupDay3Email);
    sendForWindow(
        now.minus(7, ChronoUnit.DAYS).minus(12, ChronoUnit.HOURS),
        now.minus(7, ChronoUnit.DAYS).plus(12, ChronoUnit.HOURS),
        TYPE_DAY7,
        "Reengajamento",
        EmailTemplates::postSignupDay7Email);
  }

  private void sendForWindow(
      Instant windowStart,
      Instant windowEnd,
      String emailType,
      String subjectLabel,
      EmailTemplateFn templateFn) {
    List<User> users = userRepo.findByCreatedAtBetween(windowStart, windowEnd);
    for (User user : users) {
      if (sentRepo.existsByUserIdAndEmailType(user.getId(), emailType)) {
        continue;
      }
      String tenantName =
          tenantRepo.findById(user.getTenantId()).map(t -> t.getName()).orElse("sua organização");
      try {
        emailSender.send(
            user.getEmail(),
            subjectLabel + " – " + EmailTemplates.PRODUCT_DISPLAY_NAME,
            templateFn.apply(user.getName(), tenantName));
        sentRepo.recordSent(user.getId(), emailType);
        log.info("Post-signup {} email sent to user {}", emailType, user.getId());
      } catch (Exception e) {
        log.warn(
            "Failed to send post-signup {} email to user {}: {}",
            emailType,
            user.getId(),
            e.getMessage());
      }
    }
  }

  @FunctionalInterface
  private interface EmailTemplateFn {
    String apply(String userName, String tenantName);
  }
}
