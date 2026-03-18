package com.union.solutions.saascore.unit.application.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.union.solutions.saascore.application.email.EmailTemplates;
import com.union.solutions.saascore.application.port.AuditLogger;
import com.union.solutions.saascore.application.port.EmailSender;
import com.union.solutions.saascore.application.port.OutboxPublisherPort;
import com.union.solutions.saascore.application.port.TenantRepository;
import com.union.solutions.saascore.application.port.UserRepository;
import com.union.solutions.saascore.application.user.UserAlreadyExistsException;
import com.union.solutions.saascore.application.user.UserManagementUseCase;
import com.union.solutions.saascore.config.TenantContext;
import com.union.solutions.saascore.domain.Tenant;
import com.union.solutions.saascore.domain.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserManagementUseCaseTest {

  private static final UUID PLATFORM_TENANT_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");

  @Mock UserRepository userRepo;
  @Mock TenantRepository tenantRepo;
  @Mock OutboxPublisherPort outboxPublisher;
  @Mock AuditLogger auditLogger;
  @Mock EmailSender emailSender;
  @Mock PasswordEncoder passwordEncoder;

  private UserManagementUseCase useCase;

  @BeforeEach
  void setUp() {
    TenantContext.setSubject("admin@test.com");
    TenantContext.setRoles(List.of("admin"));
    TenantContext.setPerms(List.of("user:write"));
    TenantContext.setCorrelationId("test-correlation-id");
    useCase =
        new UserManagementUseCase(
            userRepo,
            tenantRepo,
            outboxPublisher,
            auditLogger,
            emailSender,
            passwordEncoder,
            "https://app.test");
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Test
  void invite_whenTenantNameIsSystem_sendsEmailWithFluxeB2BSuite() {
    UUID tenantId = UUID.randomUUID();
    Tenant tenant = new Tenant(tenantId, "System", "pro", "us-east-1", null, null, null);
    when(tenantRepo.findById(tenantId)).thenReturn(Optional.of(tenant));
    when(userRepo.findByEmailAndTenantId(eq("new@example.com"), eq(tenantId)))
        .thenReturn(Optional.empty());
    when(passwordEncoder.encode(anyString())).thenReturn("encoded");
    when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    useCase.invite(tenantId, "New User", "new@example.com", List.of("member"));

    ArgumentCaptor<String> toCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
    verify(emailSender).send(toCaptor.capture(), subjectCaptor.capture(), bodyCaptor.capture());

    assertThat(toCaptor.getValue()).isEqualTo("new@example.com");
    assertThat(subjectCaptor.getValue()).contains(EmailTemplates.PRODUCT_DISPLAY_NAME);
    assertThat(subjectCaptor.getValue()).doesNotContain("System");
    assertThat(bodyCaptor.getValue()).contains(EmailTemplates.PRODUCT_DISPLAY_NAME);
    assertThat(bodyCaptor.getValue()).doesNotContain(">System<");
  }

  @Test
  void invite_whenTenantIsPlatform_sendsEmailWithFluxeB2BSuite() {
    Tenant tenant = new Tenant(PLATFORM_TENANT_ID, "Admin Console", "pro", "us", null, null, null);
    when(tenantRepo.findById(PLATFORM_TENANT_ID)).thenReturn(Optional.of(tenant));
    when(userRepo.findByEmailAndTenantId(eq("u@test.com"), eq(PLATFORM_TENANT_ID)))
        .thenReturn(Optional.empty());
    when(passwordEncoder.encode(anyString())).thenReturn("encoded");
    when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    useCase.invite(PLATFORM_TENANT_ID, "User", "u@test.com", List.of("admin"));

    ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
    verify(emailSender).send(anyString(), subjectCaptor.capture(), bodyCaptor.capture());
    assertThat(subjectCaptor.getValue()).contains(EmailTemplates.PRODUCT_DISPLAY_NAME);
    assertThat(bodyCaptor.getValue()).contains(EmailTemplates.PRODUCT_DISPLAY_NAME);
  }

  @Test
  void invite_whenTenantNameIsAcme_sendsEmailWithAcme() {
    UUID tenantId = UUID.randomUUID();
    Tenant tenant = new Tenant(tenantId, "Acme Distribuidora", "pro", "us", null, null, null);
    when(tenantRepo.findById(tenantId)).thenReturn(Optional.of(tenant));
    when(userRepo.findByEmailAndTenantId(eq("a@acme.com"), eq(tenantId)))
        .thenReturn(Optional.empty());
    when(passwordEncoder.encode(anyString())).thenReturn("encoded");
    when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    useCase.invite(tenantId, "Admin Acme", "a@acme.com", List.of("admin"));

    ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
    verify(emailSender).send(anyString(), subjectCaptor.capture(), bodyCaptor.capture());
    assertThat(subjectCaptor.getValue()).contains("Acme Distribuidora");
    assertThat(bodyCaptor.getValue()).contains("Acme Distribuidora");
  }

  @Test
  void invite_userAlreadyExists_throws() {
    UUID tenantId = UUID.randomUUID();
    User existing = new User();
    existing.setEmail("existing@test.com");
    when(userRepo.findByEmailAndTenantId(eq("existing@test.com"), eq(tenantId)))
        .thenReturn(Optional.of(existing));

    assertThatThrownBy(
            () -> useCase.invite(tenantId, "Name", "existing@test.com", List.of("member")))
        .isInstanceOf(UserAlreadyExistsException.class)
        .hasMessageContaining("existing@test.com");

    verify(emailSender, never()).send(anyString(), anyString(), anyString());
  }

  @Test
  void invite_createsUserWithMustChangePasswordTrue() {
    UUID tenantId = UUID.randomUUID();
    when(tenantRepo.findById(tenantId))
        .thenReturn(Optional.of(new Tenant(tenantId, "T", null, null, null, null, null)));
    when(userRepo.findByEmailAndTenantId(anyString(), eq(tenantId))).thenReturn(Optional.empty());
    when(passwordEncoder.encode(anyString())).thenReturn("hash");
    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    when(userRepo.save(userCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

    useCase.invite(tenantId, "João", "joao@test.com", List.of("member"));

    assertThat(userCaptor.getValue().isMustChangePassword()).isTrue();
  }
}
