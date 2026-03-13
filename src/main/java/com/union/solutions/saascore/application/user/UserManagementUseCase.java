package com.union.solutions.saascore.application.user;

import com.union.solutions.saascore.application.abac.AuditLogger;
import com.union.solutions.saascore.application.port.OutboxPublisherPort;
import com.union.solutions.saascore.application.port.UserRepository;
import com.union.solutions.saascore.config.TenantContext;
import com.union.solutions.saascore.domain.User;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserManagementUseCase {

  private final UserRepository userRepo;
  private final OutboxPublisherPort outboxPublisher;
  private final AuditLogger auditLogger;

  public UserManagementUseCase(
      UserRepository userRepo, OutboxPublisherPort outboxPublisher, AuditLogger auditLogger) {
    this.userRepo = userRepo;
    this.outboxPublisher = outboxPublisher;
    this.auditLogger = auditLogger;
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

    UUID id = UUID.randomUUID();
    Instant now = Instant.now();
    User user =
        new User(
            id,
            email,
            name,
            null,
            tenantId,
            roles != null ? roles : List.of("member"),
            User.UserStatus.PENDING,
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

    return user;
  }
}
