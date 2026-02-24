package com.union.solutions.saascore.unit.application.abac;

import static org.assertj.core.api.Assertions.assertThat;

import com.union.solutions.saascore.application.abac.AbacContext;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AbacContextTest {

  @Test
  void record_preservesValues() {
    UUID tenantId = UUID.randomUUID();
    AbacContext ctx =
        new AbacContext(tenantId, "user@test", "tenants:write", "pro", "us-east-1", "corr-1");
    assertThat(ctx.tenantId()).isEqualTo(tenantId);
    assertThat(ctx.subject()).isEqualTo("user@test");
    assertThat(ctx.permission()).isEqualTo("tenants:write");
    assertThat(ctx.plan()).isEqualTo("pro");
    assertThat(ctx.region()).isEqualTo("us-east-1");
    assertThat(ctx.correlationId()).isEqualTo("corr-1");
  }
}
