package com.union.solutions.saascore.unit.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.union.solutions.saascore.config.TenantContext;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TenantContextTest {

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Test
  void setAndGetTenantId() {
    UUID id = UUID.randomUUID();
    TenantContext.setTenantId(id);
    assertThat(TenantContext.getTenantId()).isPresent().contains(id);
  }

  @Test
  void getTenantId_notSet_isEmpty() {
    assertThat(TenantContext.getTenantId()).isEmpty();
  }

  @Test
  void setAndGetSubject() {
    TenantContext.setSubject("user@test");
    assertThat(TenantContext.getSubject()).isEqualTo("user@test");
  }

  @Test
  void setAndGetPlan() {
    TenantContext.setPlan("pro");
    assertThat(TenantContext.getPlan()).isEqualTo("pro");
  }

  @Test
  void getPlan_notSet_returnsEmpty() {
    assertThat(TenantContext.getPlan()).isEmpty();
  }

  @Test
  void setAndGetRegion() {
    TenantContext.setRegion("eu-west-1");
    assertThat(TenantContext.getRegion()).isEqualTo("eu-west-1");
  }

  @Test
  void setAndGetRoles() {
    TenantContext.setRoles(List.of("admin", "user"));
    assertThat(TenantContext.getRoles()).containsExactly("admin", "user");
  }

  @Test
  void getRoles_notSet_returnsEmptyList() {
    assertThat(TenantContext.getRoles()).isEmpty();
  }

  @Test
  void setAndGetPerms() {
    TenantContext.setPerms(List.of("read", "write"));
    assertThat(TenantContext.getPerms()).containsExactly("read", "write");
  }

  @Test
  void setAndGetCorrelationId() {
    TenantContext.setCorrelationId("corr-1");
    assertThat(TenantContext.getCorrelationId()).isEqualTo("corr-1");
  }

  @Test
  void clear_removesAllValues() {
    TenantContext.setTenantId(UUID.randomUUID());
    TenantContext.setSubject("user");
    TenantContext.setPlan("pro");
    TenantContext.clear();
    assertThat(TenantContext.getTenantId()).isEmpty();
    assertThat(TenantContext.getSubject()).isNull();
    assertThat(TenantContext.getPlan()).isEmpty();
  }
}
