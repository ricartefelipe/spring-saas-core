package com.union.solutions.saascore.application.onboarding;

import com.union.solutions.saascore.application.auth.JwtTenantClaimsNormalizer;
import com.union.solutions.saascore.application.email.EmailTemplates;
import com.union.solutions.saascore.application.port.AuditLogger;
import com.union.solutions.saascore.application.port.EmailSender;
import com.union.solutions.saascore.application.port.OutboxPublisherPort;
import com.union.solutions.saascore.application.port.TenantRepository;
import com.union.solutions.saascore.application.port.UserRepository;
import com.union.solutions.saascore.application.user.EmailAlreadyExistsException;
import com.union.solutions.saascore.domain.Tenant;
import com.union.solutions.saascore.domain.User;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OnboardingUseCase {

  private final TenantRepository tenantRepo;
  private final UserRepository userRepo;
  private final PasswordEncoder passwordEncoder;
  private final OutboxPublisherPort outboxPublisher;
  private final AuditLogger auditLogger;
  private final EmailSender emailSender;

  public OnboardingUseCase(
      TenantRepository tenantRepo,
      UserRepository userRepo,
      PasswordEncoder passwordEncoder,
      OutboxPublisherPort outboxPublisher,
      AuditLogger auditLogger,
      EmailSender emailSender) {
    this.tenantRepo = tenantRepo;
    this.userRepo = userRepo;
    this.passwordEncoder = passwordEncoder;
    this.outboxPublisher = outboxPublisher;
    this.auditLogger = auditLogger;
    this.emailSender = emailSender;
  }

  @Transactional
  public OnboardingResult onboard(
      String companyName,
      String plan,
      String region,
      String adminEmail,
      String adminName,
      String rawPassword) {
    if (userRepo.existsByEmail(adminEmail)) {
      throw new EmailAlreadyExistsException(adminEmail);
    }

    UUID tenantId = UUID.randomUUID();
    Instant now = Instant.now();
    String normalizedPlan = JwtTenantClaimsNormalizer.plan(plan);
    String normalizedRegion = JwtTenantClaimsNormalizer.region(region);
    Tenant tenant =
        new Tenant(
            tenantId,
            companyName,
            normalizedPlan,
            normalizedRegion,
            Tenant.TenantStatus.ACTIVE,
            now,
            now);
    tenantRepo.save(tenant);

    UUID userId = UUID.randomUUID();
    String hash = passwordEncoder.encode(rawPassword);
    List<String> adminRoles = List.of("admin", "member");
    User user =
        new User(
            userId,
            adminEmail,
            adminName,
            hash,
            tenantId,
            adminRoles,
            User.UserStatus.ACTIVE,
            now,
            now);
    userRepo.save(user);

    outboxPublisher.publish(
        "ONBOARDING",
        tenantId.toString(),
        "onboarding.completed",
        Map.of(
            "tenantId", tenantId.toString(),
            "tenantName", companyName,
            "plan", normalizedPlan,
            "adminEmail", adminEmail));

    auditLogger.log(
        tenantId,
        adminEmail,
        adminRoles.toString(),
        "[]",
        "ONBOARDING_COMPLETED",
        "tenant",
        tenantId.toString(),
        null,
        null,
        201,
        null,
        null);

    emailSender.send(
        adminEmail,
        "Welcome to " + companyName,
        EmailTemplates.welcomeEmail(adminName, companyName));

    return new OnboardingResult(tenant, user);
  }

  public record OnboardingResult(Tenant tenant, User user) {}
}
