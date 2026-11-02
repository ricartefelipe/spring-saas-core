package com.union.solutions.saascore.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class SeedContextsMigrationTest {

  private static final String CHANGELOG = "db/changelog/db.changelog-master.yaml";

  @Container
  @SuppressWarnings("resource")
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  private Connection connect() throws SQLException {
    return DriverManager.getConnection(
        postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
  }

  @BeforeEach
  void bancoVazio() throws SQLException {
    try (Connection c = connect();
        Statement s = c.createStatement()) {
      s.execute("DROP SCHEMA public CASCADE; CREATE SCHEMA public;");
    }
  }

  private void migrar(String contexts) throws Exception {
    try (Connection c = connect()) {
      var database =
          DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(c));
      try (Liquibase liquibase =
          new Liquibase(CHANGELOG, new ClassLoaderResourceAccessor(), database)) {
        liquibase.update(new Contexts(contexts), new LabelExpression());
      }
    }
  }

  private long contar(String sql) throws SQLException {
    try (Connection c = connect();
        Statement s = c.createStatement();
        ResultSet r = s.executeQuery(sql)) {
      r.next();
      return r.getLong(1);
    }
  }

  private void executar(String sql) throws SQLException {
    try (Connection c = connect();
        Statement s = c.createStatement()) {
      s.executeUpdate(sql);
    }
  }

  private String hashDe(String email) throws SQLException {
    try (Connection c = connect();
        Statement s = c.createStatement();
        ResultSet r =
            s.executeQuery("SELECT password_hash FROM users WHERE email = '" + email + "'")) {
      return r.next() ? r.getString(1) : null;
    }
  }

  @Test
  void producaoNovaNaoCriaNenhumaConta() throws Exception {
    migrar("prod");

    assertThat(contar("SELECT COUNT(*) FROM users")).isZero();
  }

  @Test
  void pilotoEmBancoNovoMantemContasDeDemonstracao() throws Exception {
    migrar("prod,seed");

    assertThat(hashDe("admin@local")).startsWith("$2");
    assertThat(hashDe("ops@demo.example.com")).startsWith("$2");
    assertThat(hashDe("admin@acme.com.br")).startsWith("$2");
  }

  @Test
  void stagingEmBancoNovoMantemContasDeDemonstracao() throws Exception {
    migrar("staging,seed");

    assertThat(hashDe("admin@local")).startsWith("$2");
  }

  @Test
  void nenhumContextoGravaEmailPessoal() throws Exception {
    migrar("staging,seed");

    assertThat(contar("SELECT COUNT(*) FROM users WHERE email LIKE '%@gmail.com'")).isZero();
    assertThat(contar("SELECT COUNT(*) FROM audit_log WHERE actor_sub LIKE '%@gmail.com'"))
        .isZero();
  }

  @Test
  void producaoExistenteDesativaSenhasPublicasEPreservaSenhaTrocada() throws Exception {
    migrar("prod,seed");
    String trocada = "$2a$10$senhaJaTrocadaPeloDonoDaContaNaoDeveMudarAbcdefghijklm";
    executar(
        "UPDATE users SET password_hash = '" + trocada + "' WHERE email = 'compras@acme.com.br'");

    migrar("prod");

    assertThat(hashDe("admin@local")).isEqualTo("desativada");
    assertThat(hashDe("ops@demo.example.com")).isEqualTo("desativada");
    assertThat(hashDe("admin@acme.com.br")).isEqualTo("desativada");
    assertThat(hashDe("compras@acme.com.br")).isEqualTo(trocada);
  }
}
