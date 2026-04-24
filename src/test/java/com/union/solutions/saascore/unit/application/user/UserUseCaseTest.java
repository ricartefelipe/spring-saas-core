package com.union.solutions.saascore.unit.application.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.union.solutions.saascore.application.port.AuditLogger;
import com.union.solutions.saascore.application.port.EmailSender;
import com.union.solutions.saascore.application.port.OutboxPublisherPort;
import com.union.solutions.saascore.application.port.PasswordResetTokenRepository;
import com.union.solutions.saascore.application.port.TenantRepository;
import com.union.solutions.saascore.application.port.TokenIssuer;
import com.union.solutions.saascore.application.port.UserRepository;
import com.union.solutions.saascore.application.user.UserUseCase;
import com.union.solutions.saascore.domain.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserUseCaseTest {

  @Mock UserRepository userRepo;
  @Mock TenantRepository tenantRepo;
  @Mock PasswordResetTokenRepository resetTokenRepo;
  @Mock PasswordEncoder passwordEncoder;
  @Mock TokenIssuer tokenIssuer;
  @Mock OutboxPublisherPort outboxPublisher;
  @Mock AuditLogger auditLogger;
  @Mock EmailSender emailSender;

  @Test
  void authenticate_emptyWhenNoTenantAndNotPlatformAdmin() {
    UserUseCase useCase = newUserUseCase();
    Instant t = Instant.parse("2024-01-01T00:00:00Z");
    UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
    User u =
        new User(
            id,
            "member@test.com",
            "n",
            "hash",
            null,
            List.of("member"),
            User.UserStatus.ACTIVE,
            false,
            t,
            t);

    when(userRepo.findByEmail("member@test.com")).thenReturn(Optional.of(u));
    when(passwordEncoder.matches("p", "hash")).thenReturn(true);

    assertThat(useCase.authenticate("member@test.com", "p")).isEmpty();
    verify(userRepo, never()).save(any());
    verifyNoMoreInteractions(tokenIssuer);
  }

  @Test
  void authenticate_allowsNoTenantWhenRoleAdmin_usesTidStar() {
    UserUseCase useCase = newUserUseCase();
    Instant t = Instant.parse("2024-01-01T00:00:00Z");
    UUID id = UUID.fromString("22222222-2222-2222-2222-222222222222");
    User u =
        new User(
            id,
            "admin@test.com",
            "n",
            "hash",
            null,
            List.of("admin"),
            User.UserStatus.ACTIVE,
            false,
            t,
            t);

    when(userRepo.findByEmail("admin@test.com")).thenReturn(Optional.of(u));
    when(passwordEncoder.matches("p", "hash")).thenReturn(true);
    when(tokenIssuer.issue(eq("admin@test.com"), eq("*"), any(), any(), any(), any(), eq(false)))
        .thenReturn("jwt");

    assertThat(useCase.authenticate("admin@test.com", "p"))
        .isPresent()
        .get()
        .extracting(UserUseCase.AuthResult::accessToken)
        .isEqualTo("jwt");
    verify(userRepo).save(u);
  }

  private UserUseCase newUserUseCase() {
    return new UserUseCase(
        userRepo,
        tenantRepo,
        resetTokenRepo,
        passwordEncoder,
        tokenIssuer,
        outboxPublisher,
        auditLogger,
        emailSender,
        "http://localhost:4200");
  }
}
