package com.union.solutions.saascore.unit.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.union.solutions.saascore.application.port.UserRepository;
import com.union.solutions.saascore.config.PlatformAdminBootstrap;
import com.union.solutions.saascore.domain.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class PlatformAdminBootstrapTest {

  private static final String EMAIL = "admin@empresa.test";
  private static final String PASSWORD = "senha-com-doze-ou-mais";

  private final UserRepository users = mock(UserRepository.class);
  private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);

  private PlatformAdminBootstrap bootstrap(String email, String password) {
    return new PlatformAdminBootstrap(users, encoder, email, password);
  }

  @Test
  void semConfiguracaoNaoFazNada() {
    bootstrap("", "").run(null);
    verify(users, never()).save(any());
  }

  @Test
  void criaAdministradorDaPlataformaComTrocaDeSenhaObrigatoria() {
    when(users.existsByEmail(EMAIL)).thenReturn(false);

    bootstrap(EMAIL, PASSWORD).run(null);

    ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
    verify(users).save(saved.capture());
    User admin = saved.getValue();
    assertThat(admin.getEmail()).isEqualTo(EMAIL);
    assertThat(admin.getTenantId().toString()).isEqualTo("00000000-0000-0000-0000-000000000001");
    assertThat(admin.getRoles()).containsExactly("admin");
    assertThat(admin.isMustChangePassword()).isTrue();
    assertThat(encoder.matches(PASSWORD, admin.getPasswordHash())).isTrue();
  }

  @Test
  void naoAlteraContaExistente() {
    when(users.existsByEmail(EMAIL)).thenReturn(true);

    bootstrap(EMAIL, PASSWORD).run(null);

    verify(users, never()).save(any());
  }

  @Test
  void recusaConfiguracaoIncompletaOuSenhaCurta() {
    assertThatThrownBy(() -> bootstrap(EMAIL, "curta").run(null))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> bootstrap("", PASSWORD).run(null))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> bootstrap(EMAIL, "").run(null))
        .isInstanceOf(IllegalStateException.class);
  }
}
