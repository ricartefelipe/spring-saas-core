package com.union.solutions.saascore.application.user;

import com.union.solutions.saascore.application.auth.JwtRolePermissions;
import com.union.solutions.saascore.application.email.EmailTemplates;
import com.union.solutions.saascore.application.port.AuditLogger;
import com.union.solutions.saascore.application.port.EmailSender;
import com.union.solutions.saascore.application.port.OutboxPublisherPort;
import com.union.solutions.saascore.application.port.PasswordResetTokenRepository;
import com.union.solutions.saascore.application.port.TenantRepository;
import com.union.solutions.saascore.application.port.TokenIssuer;
import com.union.solutions.saascore.application.port.UserRepository;
import com.union.solutions.saascore.config.TenantContext;
import com.union.solutions.saascore.domain.PasswordResetToken;
import com.union.solutions.saascore.domain.Tenant;
import com.union.solutions.saascore.domain.User;
import com.union.solutions.saascore.domain.exception.CryptoException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserUseCase {

  private static final Logger log = LoggerFactory.getLogger(UserUseCase.class);
  private static final long RESET_TOKEN_TTL_SECONDS = 3600;

  private final UserRepository userRepo;
  private final TenantRepository tenantRepo;
  private final PasswordResetTokenRepository resetTokenRepo;
  private final PasswordEncoder passwordEncoder;
  private final TokenIssuer tokenIssuer;
  private final OutboxPublisherPort outboxPublisher;
  private final AuditLogger auditLogger;
  private final EmailSender emailSender;
  private final String frontendUrl;
  private final SecureRandom secureRandom = new SecureRandom();

  public UserUseCase(
      UserRepository userRepo,
      TenantRepository tenantRepo,
      PasswordResetTokenRepository resetTokenRepo,
      PasswordEncoder passwordEncoder,
      TokenIssuer tokenIssuer,
      OutboxPublisherPort outboxPublisher,
      AuditLogger auditLogger,
      EmailSender emailSender,
      @Value("${app.email.frontend-url:http://localhost:4200}") String frontendUrl) {
    this.userRepo = userRepo;
    this.tenantRepo = tenantRepo;
    this.resetTokenRepo = resetTokenRepo;
    this.passwordEncoder = passwordEncoder;
    this.tokenIssuer = tokenIssuer;
    this.outboxPublisher = outboxPublisher;
    this.auditLogger = auditLogger;
    this.emailSender = emailSender;
    this.frontendUrl = frontendUrl;
  }

  @Transactional
  public User register(
      String email, String name, String rawPassword, UUID tenantId, List<String> roles) {
    if (userRepo.existsByEmail(email)) {
      throw new EmailAlreadyExistsException(email);
    }

    UUID id = UUID.randomUUID();
    Instant now = Instant.now();
    String hash = passwordEncoder.encode(rawPassword);

    User user =
        new User(
            id,
            email,
            name,
            hash,
            tenantId,
            roles != null ? roles : List.of("member"),
            User.UserStatus.ACTIVE,
            now,
            now);

    userRepo.save(user);

    outboxPublisher.publish(
        "USER",
        id.toString(),
        "user.registered",
        Map.of("email", email, "name", name, "tenantId", tenantId.toString()));

    auditLogger.log(
        tenantId,
        email,
        "[]",
        "[]",
        "USER_REGISTERED",
        "user",
        id.toString(),
        null,
        null,
        201,
        TenantContext.getCorrelationId(),
        null);

    return user;
  }

  private String buildAccessToken(User u) {
    List<String> perms = JwtRolePermissions.forRoles(u.getRoles());
    Optional<Tenant> tenant = tenantRepo.findById(u.getTenantId());
    String plan = tenant.map(Tenant::getPlan).orElse("starter");
    String region = tenant.map(Tenant::getRegion).orElse("us-east-1");
    return tokenIssuer.issue(
        u.getEmail(),
        u.getTenantId().toString(),
        u.getRoles(),
        perms,
        plan,
        region,
        u.isMustChangePassword());
  }

  @Transactional
  public Optional<AuthResult> authenticate(String email, String rawPassword) {
    return userRepo
        .findByEmail(email)
        .filter(User::isActive)
        .filter(u -> passwordEncoder.matches(rawPassword, u.getPasswordHash()))
        .map(
            u -> {
              Instant now = Instant.now();
              u.setLastLoginAt(now);
              u.setUpdatedAt(now);
              userRepo.save(u);
              return new AuthResult(buildAccessToken(u), u, u.isMustChangePassword());
            });
  }

  /**
   * Altera a senha do usuário autenticado (obrigatório após login com senha temporária). Requer a
   * senha atual. Ao concluir, limpa o flag mustChangePassword.
   */
  /**
   * @return new access token (JWT without {@code mcp}) on success; empty if credentials or context
   *     invalid
   */
  @Transactional
  public Optional<String> changePassword(String currentPassword, String newPassword) {
    Optional<UUID> tenantIdOpt = TenantContext.getTenantId();
    String subject = TenantContext.getSubject();
    if (tenantIdOpt.isEmpty() || subject == null || subject.isBlank()) {
      return Optional.empty();
    }
    return userRepo
        .findByEmailAndTenantId(subject, tenantIdOpt.get())
        .filter(u -> passwordEncoder.matches(currentPassword, u.getPasswordHash()))
        .map(
            u -> {
              u.setPasswordHash(passwordEncoder.encode(newPassword));
              u.setMustChangePassword(false);
              u.setUpdatedAt(Instant.now());
              userRepo.save(u);
              auditLogger.log(
                  u.getTenantId(),
                  u.getEmail(),
                  "[]",
                  "[]",
                  "USER_PASSWORD_CHANGED",
                  "user",
                  u.getId().toString(),
                  null,
                  null,
                  200,
                  TenantContext.getCorrelationId(),
                  null);
              return buildAccessToken(u);
            });
  }

  @Transactional
  public PasswordResetResult requestPasswordReset(String email) {
    Optional<User> maybeUser = userRepo.findByEmail(email);
    if (maybeUser.isEmpty()) {
      return new PasswordResetResult(true);
    }

    User user = maybeUser.get();
    byte[] rawBytes = new byte[32];
    secureRandom.nextBytes(rawBytes);
    String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(rawBytes);
    String tokenHash = sha256(rawToken);

    UUID tokenId = UUID.randomUUID();
    Instant now = Instant.now();
    PasswordResetToken resetToken =
        new PasswordResetToken(
            tokenId, user.getId(), tokenHash, false, now.plusSeconds(RESET_TOKEN_TTL_SECONDS), now);

    resetTokenRepo.save(resetToken);

    outboxPublisher.publish(
        "USER",
        user.getId().toString(),
        "user.password_reset_requested",
        Map.of(
            "userId",
            user.getId().toString(),
            "tenantId",
            user.getTenantId().toString(),
            "tokenId",
            tokenId.toString(),
            "rawToken",
            rawToken));

    String resetLink = frontendUrl + "/reset-password?tokenId=" + tokenId + "&token=" + rawToken;
    emailSender.send(
        user.getEmail(),
        "Reset your password",
        EmailTemplates.passwordResetEmail(user.getName(), resetLink));

    log.info("Password reset requested for user={}", user.getId());
    return new PasswordResetResult(true, tokenId, rawToken);
  }

  @Transactional
  public boolean resetPassword(UUID tokenId, String rawToken, String newPassword) {
    Optional<PasswordResetToken> maybeToken = resetTokenRepo.findByIdAndNotUsed(tokenId);
    if (maybeToken.isEmpty()) {
      return false;
    }

    PasswordResetToken resetToken = maybeToken.get();
    if (!resetToken.isValid()) {
      return false;
    }

    String tokenHash = sha256(rawToken);
    if (!tokenHash.equals(resetToken.getTokenHash())) {
      return false;
    }

    Optional<User> maybeUser = userRepo.findById(resetToken.getUserId());
    if (maybeUser.isEmpty()) {
      return false;
    }

    User user = maybeUser.get();
    user.setPasswordHash(passwordEncoder.encode(newPassword));
    user.setUpdatedAt(Instant.now());
    userRepo.save(user);

    resetToken.setUsed(true);
    resetTokenRepo.save(resetToken);

    auditLogger.log(
        user.getTenantId(),
        user.getEmail(),
        "[]",
        "[]",
        "USER_PASSWORD_RESET",
        "user",
        user.getId().toString(),
        null,
        null,
        200,
        TenantContext.getCorrelationId(),
        null);

    return true;
  }

  @Transactional(readOnly = true)
  public Optional<User> getById(UUID id) {
    return userRepo.findById(id);
  }

  @Transactional(readOnly = true)
  public List<User> getByTenantId(UUID tenantId) {
    return userRepo.findByTenantId(tenantId);
  }

  private String sha256(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new CryptoException("SHA-256 not available", e);
    }
  }

  public record AuthResult(String accessToken, User user, boolean mustChangePassword) {}

  public record PasswordResetResult(boolean accepted, UUID tokenId, String rawToken) {
    public PasswordResetResult(boolean accepted) {
      this(accepted, null, null);
    }
  }
}
