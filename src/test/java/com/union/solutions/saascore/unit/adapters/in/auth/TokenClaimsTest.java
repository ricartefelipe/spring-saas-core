package com.union.solutions.saascore.unit.adapters.in.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.union.solutions.saascore.adapters.in.auth.TokenClaims;
import java.util.List;
import org.junit.jupiter.api.Test;

class TokenClaimsTest {

  @Test
  void empty_returnsNullAndEmptyCollections() {
    TokenClaims empty = TokenClaims.empty();
    assertThat(empty.sub()).isNull();
    assertThat(empty.tid()).isNull();
    assertThat(empty.roles()).isEmpty();
    assertThat(empty.perms()).isEmpty();
    assertThat(empty.plan()).isEmpty();
    assertThat(empty.region()).isEmpty();
  }

  @Test
  void constructor_preservesValues() {
    TokenClaims claims =
        new TokenClaims(
            "user@test", "tid-1", List.of("admin"), List.of("read"), "pro", "us-east-1");
    assertThat(claims.sub()).isEqualTo("user@test");
    assertThat(claims.tid()).isEqualTo("tid-1");
    assertThat(claims.roles()).containsExactly("admin");
    assertThat(claims.perms()).containsExactly("read");
    assertThat(claims.plan()).isEqualTo("pro");
    assertThat(claims.region()).isEqualTo("us-east-1");
  }
}
