package com.union.solutions.saascore.application.email;

import com.union.solutions.saascore.application.port.EmailSender;
import com.union.solutions.saascore.application.port.ReactivationSentRepository;
import com.union.solutions.saascore.application.port.TenantRepository;
import com.union.solutions.saascore.application.port.UserRepository;
import com.union.solutions.saascore.domain.Tenant;
import com.union.solutions.saascore.domain.User;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sends reactivation emails to tenants with no login for a configured number of days. Sends to the
 * first admin user of the tenant. Does not send again to the same tenant within the cooldown days.
 */
@Component
public class ReactivationEmailScheduler {

  private static final Logger log = LoggerFactory.getLogger(ReactivationEmailScheduler.class);

  private final TenantRepository tenantRepo;
  private final UserRepository userRepo;
  private final EmailSender emailSender;
  private final ReactivationSentRepository reactivationSentRepo;
  private final String frontendUrl;
  private final int inactiveDays;
  private final int cooldownDays;

  public ReactivationEmailScheduler(
      TenantRepository tenantRepo,
      UserRepository userRepo,
      EmailSender emailSender,
      ReactivationSentRepository reactivationSentRepo,
      @Value("${app.email.frontend-url:http://localhost:4200}") String frontendUrl,
      @Value("${app.reactivation.inactive-days:14}") int inactiveDays,
      @Value("${app.reactivation.cooldown-days:30}") int cooldownDays) {
    this.tenantRepo = tenantRepo;
    this.userRepo = userRepo;
    this.emailSender = emailSender;
    this.reactivationSentRepo = reactivationSentRepo;
    this.frontendUrl = frontendUrl.endsWith("/") ? frontendUrl : frontendUrl + "/";
    this.inactiveDays = inactiveDays;
    this.cooldownDays = cooldownDays;
  }

  @Scheduled(cron = "${app.reactivation.cron:0 0 10 * * *}")
  @Transactional
  public void sendReactivationEmails() {
    Instant cutoff = Instant.now().minus(inactiveDays, ChronoUnit.DAYS);
    Instant cooldownAfter = Instant.now().minus(cooldownDays, ChronoUnit.DAYS);
    String loginUrl = frontendUrl + "login";

    List<UUID> tenantIds = tenantRepo.findAllActiveIds();
    for (UUID tenantId : tenantIds) {
      try {
        Optional<Instant> maxLogin = userRepo.findMaxLastLoginAtByTenantId(tenantId);
        boolean inactive = maxLogin.isEmpty() || maxLogin.get().isBefore(cutoff);
        if (!inactive) continue;

        if (reactivationSentRepo.wasSentAfter(tenantId, cooldownAfter)) continue;

        Optional<Tenant> tenantOpt = tenantRepo.findById(tenantId);
        if (tenantOpt.isEmpty()) continue;

        List<User> users = userRepo.findByTenantId(tenantId);
        User recipient =
            users.stream()
                .filter(u -> u.getRoles() != null && u.getRoles().contains("admin"))
                .findFirst()
                .orElse(users.isEmpty() ? null : users.get(0));
        if (recipient == null) continue;

        Tenant tenant = tenantOpt.get();
        String tenantName = tenant.getName() != null ? tenant.getName() : "sua empresa";
        emailSender.send(
            recipient.getEmail(),
            "Reative sua conta – " + EmailTemplates.PRODUCT_DISPLAY_NAME,
            EmailTemplates.reactivationEmail(recipient.getName(), tenantName, loginUrl));
        reactivationSentRepo.record(tenantId, Instant.now());
        log.info(
            "Reactivation email sent to tenant={} recipient={}", tenantId, recipient.getEmail());
      } catch (Exception e) {
        log.warn("Failed to send reactivation email for tenant {}: {}", tenantId, e.getMessage());
      }
    }
  }
}
