package com.union.solutions.saascore.unit.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * As releases 1.3.x a 1.4.1 abriram a entrada no CHANGELOG sem mudar a versão do pom.xml, e o
 * OpenAPI e /api/v1/ai/docs seguiram anunciando 1.0.0. A versão vem do pom.xml; estes testes a
 * prendem ao CHANGELOG, ao contrato publicado e ao application.yml empacotado, então uma release
 * que esquecer dela falha na CI.
 */
class VersionAlignmentTest {

  private static String read(String file) throws IOException {
    return Files.readString(Path.of(file), StandardCharsets.UTF_8);
  }

  private static String group(String regex, String text) {
    Matcher m = Pattern.compile(regex, Pattern.MULTILINE).matcher(text);
    assertThat(m.find()).as("padrão %s", regex).isTrue();
    return m.group(1);
  }

  private static String pomVersion() throws IOException {
    return group(
        "<artifactId>spring-saas-core</artifactId>\\s*<version>([^<]+)</version>", read("pom.xml"));
  }

  @Test
  void pomVersion_isTheLatestChangelogEntry() throws IOException {
    String latest = group("^## \\[(\\d+\\.\\d+\\.\\d+)\\]", read("CHANGELOG.md"));
    assertThat(pomVersion()).isEqualTo(latest);
  }

  @Test
  void publishedOpenApi_announcesThePomVersion() throws IOException {
    String spec = read("docs/api/openapi.json");
    assertThat(group("\"info\"\\s*:\\s*\\{[^}]*?\"version\"\\s*:\\s*\"([^\"]+)\"", spec))
        .isEqualTo(pomVersion());
  }

  @Test
  void packagedApplicationYaml_defaultsToThePomVersion() throws IOException {
    try (InputStream in = getClass().getResourceAsStream("/application.yml")) {
      assertThat(in).isNotNull();
      String yaml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
      assertThat(yaml).contains("version: ${APP_VERSION:" + pomVersion() + "}");
    }
  }
}
