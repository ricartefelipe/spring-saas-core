package com.union.solutions.saascore.application.user;

import com.union.solutions.saascore.application.email.EmailTemplates;
import com.union.solutions.saascore.application.port.AuditLogger;
import com.union.solutions.saascore.application.port.EmailSender;
import com.union.solutions.saascore.application.port.OutboxPublisherPort;
import com.union.solutions.saascore.application.port.TenantRepository;
import com.union.solutions.saascore.application.port.UserRepository;
import com.union.solutions.saascore.config.TenantContext;
import com.union.solutions.saascore.domain.User;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserManagementUseCase {

  private static final String TEMP_PASSWORD_CHARS =
      "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
  private static final int TEMP_PASSWORD_LENGTH = 12;
  private static final SecureRandom RNG = new SecureRandom();

  private final UserRepository userRepo;
  private final TenantRepository tenantRepo;
  private final OutboxPublisherPort outboxPublisher;
  private final AuditLogger auditLogger;
  private final EmailSender emailSender;
  private final PasswordEncoder passwordEncoder;
  private final String frontendUrl;

  public UserManagementUseCase(
      UserRepository userRepo,
      TenantRepository tenantRepo,
      OutboxPublisherPort outboxPublisher,
      AuditLogger auditLogger,
      EmailSender emailSender,
      PasswordEncoder passwordEncoder,
      @Value("${app.email.frontend-url:http://localhost:4200}") String frontendUrl) {
    this.userRepo = userRepo;
    this.tenantRepo = tenantRepo;
    this.outboxPublisher = outboxPublisher;
    this.auditLogger = auditLogger;
    this.emailSender = emailSender;
    this.passwordEncoder = passwordEncoder;
    this.frontendUrl = frontendUrl;
  }

  private static String generateTemporaryPassword() {
    StringBuilder sb = new StringBuilder(TEMP_PASSWORD_LENGTH);
    for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
      sb.append(TEMP_PASSWORD_CHARS.charAt(RNG.nextInt(TEMP_PASSWORD_CHARS.length())));
    }
    return sb.toString();
  }

  @Transactional(readOnly = true)
  public List<User> listByTenant(UUID tenantId) {
    return userRepo.findByTenantId(tenantId);
  }

  @Transactional(readOnly = true)
  public Optional<User> getById(UUID id, UUID tenantId) {
    return userRepo.findByIdAndTenantId(id, tenantId);
  }

  @Transactional
  public Optional<User> update(
      UUID id, UUID tenantId, String name, List<String> roles, User.UserStatus status) {
    return userRepo
        .findByIdAndTenantId(id, tenantId)
        .map(
            user -> {
              if (name != null) user.setName(name);
              if (roles != null) user.setRoles(roles);
              if (status != null) user.setStatus(status);
              user.setUpdatedAt(Instant.now());
              User saved = userRepo.save(user);
              outboxPublisher.publish(
                  "USER",
                  id.toString(),
                  "user.updated",
                  Map.of("tenantId", tenantId.toString(), "name", saved.getName()));
              auditLogger.log(
                  tenantId,
                  TenantContext.getSubject(),
                  TenantContext.getRoles().toString(),
                  TenantContext.getPerms().toString(),
                  "USER_UPDATED",
                  "user",
                  id.toString(),
                  null,
                  null,
                  200,
                  TenantContext.getCorrelationId(),
                  null);
              return saved;
            });
  }

  @Transactional
  public boolean softDelete(UUID id, UUID tenantId) {
    return userRepo
        .findByIdAndTenantId(id, tenantId)
        .map(
            user -> {
              user.setStatus(User.UserStatus.DELETED);
              user.setUpdatedAt(Instant.now());
              userRepo.save(user);
              outboxPublisher.publish(
                  "USER",
                  id.toString(),
                  "user.deleted",
                  Map.of("tenantId", tenantId.toString(), "email", user.getEmail()));
              auditLogger.log(
                  tenantId,
                  TenantContext.getSubject(),
                  TenantContext.getRoles().toString(),
                  TenantContext.getPerms().toString(),
                  "USER_DELETED",
                  "user",
                  id.toString(),
                  null,
                  null,
                  204,
                  TenantContext.getCorrelationId(),
                  null);
              return true;
            })
        .orElse(false);
  }

  @Transactional
  public User invite(UUID tenantId, String name, String email, List<String> roles) {
    Optional<User> existing = userRepo.findByEmailAndTenantId(email, tenantId);
    if (existing.isPresent()) {
      throw new UserAlreadyExistsException(email, tenantId);
    }

    String temporaryPassword = generateTemporaryPassword();
    String passwordHash = passwordEncoder.encode(temporaryPassword);

    UUID id = UUID.randomUUID();
    Instant now = Instant.now();
    User user =
        new User(
            id,
            email,
            name,
            passwordHash,
            tenantId,
            roles != null ? roles : List.of("member"),
            User.UserStatus.ACTIVE,
            now,
            now);
    userRepo.save(user);

    outboxPublisher.publish(
        "USER",
        id.toString(),
        "user.invited",
        Map.of(
            "tenantId", tenantId.toString(),
            "email", email,
            "name", name));

    auditLogger.log(
        tenantId,
        TenantContext.getSubject(),
        TenantContext.getRoles().toString(),
        TenantContext.getPerms().toString(),
        "USER_INVITED",
        "user",
        id.toString(),
        null,
        null,
        201,
        TenantContext.getCorrelationId(),
        null);

    String tenantName =
        tenantRepo.findById(tenantId).map(t -> t.getName()).orElse("your organization");
    String inviteLink = frontendUrl + "/login";
    emailSender.send(
        email,
        "You've been invited to " + tenantName,
        EmailTemplates.inviteEmail(name, tenantName, inviteLink, temporaryPassword));

    return user;
  }
}
