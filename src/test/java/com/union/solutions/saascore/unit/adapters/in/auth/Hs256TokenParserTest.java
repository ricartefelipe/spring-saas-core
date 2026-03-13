package com.union.solutions.saascore.unit.adapters.in.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.union.solutions.saascore.adapters.in.auth.Hs256TokenParser;
import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class Hs256TokenParserTest {

  private static final String CURRENT_SECRET = "current-secret-min-32-chars-for-hs256";
  private static final String PREVIOUS_SECRET = "previous-secret-min-32-chars-for-hs256";

  private static SecretKey key(String secret) {
    return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
  }

  private static String token(SecretKey key, String sub, String tid) {
    var now = java.time.Instant.now();
    return Jwts.builder()
        .subject(sub)
        .claim("tid", tid)
        .claim("roles", List.of("admin"))
        .claim("perms", List.of())
        .claim("plan", "pro")
        .claim("region", "us")
        .issuedAt(java.util.Date.from(now))
        .expiration(java.util.Date.from(now.plusSeconds(3600)))
        .signWith(key)
        .compact();
  }

  @Test
  void parse_withCurrentKey_returnsCurrent() {
    SecretKey current = key(CURRENT_SECRET);
    String token = token(current, "user@test.com", "tid-123");
    var parser = new Hs256TokenParser(current, "");

    var result = parser.parse(token);

    assertThat(result).isPresent();
    assertThat(result.get().verifiedWithPreviousKey()).isFalse();
    assertThat(result.get().claims().sub()).isEqualTo("user@test.com");
    assertThat(result.get().claims().tid()).isEqualTo("tid-123");
  }

  @Test
  void parse_withPreviousKeyWhenConfigured_returnsPrevious() {
    SecretKey current = key(CURRENT_SECRET);
    SecretKey previous = key(PREVIOUS_SECRET);
    String token = token(previous, "olduser@test.com", "tid-456");
    var parser = new Hs256TokenParser(current, PREVIOUS_SECRET);

    var result = parser.parse(token);

    assertThat(result).isPresent();
    assertThat(result.get().verifiedWithPreviousKey()).isTrue();
    assertThat(result.get().claims().sub()).isEqualTo("olduser@test.com");
    assertThat(result.get().claims().tid()).isEqualTo("tid-456");
  }

  @Test
  void parse_withPreviousKeyWhenNotConfigured_returnsEmpty() {
    SecretKey current = key(CURRENT_SECRET);
    SecretKey previous = key(PREVIOUS_SECRET);
    String token = token(previous, "olduser@test.com", "tid-456");
    var parser = new Hs256TokenParser(current, "");

    var result = parser.parse(token);

    assertThat(result).isEmpty();
  }

  @Test
  void parse_invalidToken_returnsEmpty() {
    var parser = new Hs256TokenParser(key(CURRENT_SECRET), "");

    assertThat(parser.parse("invalid.token.here")).isEmpty();
    assertThat(parser.parse("")).isEmpty();
  }
}
