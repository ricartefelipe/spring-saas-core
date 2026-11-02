package com.union.solutions.saascore.config;

import com.union.solutions.saascore.application.port.UserRepository;
import com.union.solutions.saascore.domain.User;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Cria o primeiro administrador da plataforma a partir da configuração, sem credencial no código.
 */
@Component
public class PlatformAdminBootstrap implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(PlatformAdminBootstrap.class);
  private static final UUID PLATFORM_TENANT_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final int MIN_PASSWORD_LENGTH = 12;

  private final UserRepository users;
  private final PasswordEncoder passwordEncoder;
  private final String email;
  private final String password;

  public PlatformAdminBootstrap(
      UserRepository users,
      PasswordEncoder passwordEncoder,
      @Value("${app.bootstrap.admin.email:}") String email,
      @Value("${app.bootstrap.admin.password:}") String password) {
    this.users = users;
    this.passwordEncoder = passwordEncoder;
    this.email = email;
    this.password = password;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (email.isBlank() && password.isBlank()) {
      return;
    }
    if (email.isBlank() || password.length() < MIN_PASSWORD_LENGTH) {
      throw new IllegalStateException(
          "APP_BOOTSTRAP_ADMIN_EMAIL exige APP_BOOTSTRAP_ADMIN_PASSWORD com ao menos "
              + MIN_PASSWORD_LENGTH
              + " caracteres");
    }
    if (users.existsByEmail(email)) {
      log.info("Administrador inicial já existe; nada a criar");
      return;
    }
    Instant now = Instant.now();
    User admin =
        new User(
            UUID.randomUUID(),
            email,
            "Administração da plataforma",
            passwordEncoder.encode(password),
            PLATFORM_TENANT_ID,
            List.of("admin"),
            User.UserStatus.ACTIVE,
            now,
            now);
    admin.setMustChangePassword(true);
    users.save(admin);
    log.info("Administrador inicial criado; a senha deve ser trocada no primeiro login");
  }
}
