package com.union.solutions.saascore.unit.adapters.in.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.union.solutions.saascore.adapters.in.rest.UserController;
import com.union.solutions.saascore.application.abac.AbacEvaluator;
import com.union.solutions.saascore.application.user.InviteOutcome;
import com.union.solutions.saascore.application.user.ResendInviteOutcome;
import com.union.solutions.saascore.application.user.UserManagementUseCase;
import com.union.solutions.saascore.config.ProblemDetailsConfig;
import com.union.solutions.saascore.config.TenantContext;
import com.union.solutions.saascore.domain.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

  @Mock UserManagementUseCase userUseCase;
  @Mock AbacEvaluator abacEvaluator;

  @AfterEach
  void clearTenant() {
    TenantContext.clear();
  }

  @Test
  void invite_newUser_returns201AndBody() throws Exception {
    UUID tenantId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    TenantContext.setTenantId(tenantId);

    User invited =
        new User(
            userId,
            "novo.usuario@test.example.com",
            "Novo Usuário",
            "hash",
            tenantId,
            List.of("member"),
            User.UserStatus.ACTIVE,
            true,
            Instant.now(),
            Instant.now());

    org.mockito.Mockito.when(
            userUseCase.invite(
                org.mockito.ArgumentMatchers.eq(tenantId),
                org.mockito.ArgumentMatchers.eq("Novo Usuário"),
                org.mockito.ArgumentMatchers.eq("novo.usuario@test.example.com"),
                org.mockito.ArgumentMatchers.eq(List.of("member"))))
        .thenReturn(InviteOutcome.withPasswordForLog(invited, "TempPass123"));

    MockMvc mvc =
        MockMvcBuilders.standaloneSetup(new UserController(userUseCase, abacEvaluator)).build();

    mvc.perform(
            post("/v1/users/invite")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"name\":\"Novo Usuário\",\"email\":\"novo.usuario@test.example.com\",\"roles\":[\"member\"]}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(userId.toString()))
        .andExpect(jsonPath("$.email").value("novo.usuario@test.example.com"))
        .andExpect(jsonPath("$.name").value("Novo Usuário"))
        .andExpect(jsonPath("$.roles[0]").value("member"))
        .andExpect(jsonPath("$.temporaryPassword").value("TempPass123"));
  }

  @Test
  void invite_abacDeny_returns403() throws Exception {
    UUID tenantId = UUID.randomUUID();
    TenantContext.setTenantId(tenantId);

    org.mockito.Mockito.doThrow(new AccessDeniedException("ABAC denied"))
        .when(abacEvaluator)
        .enforceAnyOrThrow(
            org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());

    MockMvc mvc =
        MockMvcBuilders.standaloneSetup(new UserController(userUseCase, abacEvaluator))
            .setControllerAdvice(new ProblemDetailsConfig())
            .build();

    mvc.perform(
            post("/v1/users/invite")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"X\",\"email\":\"x@test.com\",\"roles\":[\"member\"]}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void resendInvite_whenFound_returns200WithTemporaryPassword() throws Exception {
    UUID tenantId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    TenantContext.setTenantId(tenantId);

    org.mockito.Mockito.when(
            userUseCase.resendInvite(
                org.mockito.ArgumentMatchers.eq(tenantId), org.mockito.ArgumentMatchers.eq(userId)))
        .thenReturn(Optional.of(ResendInviteOutcome.withPasswordForLog("NewTemp99")));

    MockMvc mvc =
        MockMvcBuilders.standaloneSetup(new UserController(userUseCase, abacEvaluator)).build();

    mvc.perform(post("/v1/users/" + userId + "/resend-invite"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.temporaryPassword").value("NewTemp99"));
  }

  @Test
  void resendInvite_whenUserMissing_returns404() throws Exception {
    UUID tenantId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    TenantContext.setTenantId(tenantId);

    org.mockito.Mockito.when(
            userUseCase.resendInvite(
                org.mockito.ArgumentMatchers.eq(tenantId), org.mockito.ArgumentMatchers.eq(userId)))
        .thenReturn(Optional.empty());

    MockMvc mvc =
        MockMvcBuilders.standaloneSetup(new UserController(userUseCase, abacEvaluator)).build();

    mvc.perform(post("/v1/users/" + userId + "/resend-invite")).andExpect(status().isNotFound());
  }
}
