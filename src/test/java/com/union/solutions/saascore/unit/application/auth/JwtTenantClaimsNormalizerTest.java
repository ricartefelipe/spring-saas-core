package com.union.solutions.saascore.unit.application.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.union.solutions.saascore.application.auth.JwtTenantClaimsNormalizer;
import org.junit.jupiter.api.Test;

class JwtTenantClaimsNormalizerTest {

  @Test
  void plan_nullOrBlank_defaultsToStarter() {
    assertThat(JwtTenantClaimsNormalizer.plan(null)).isEqualTo("starter");
    assertThat(JwtTenantClaimsNormalizer.plan("")).isEqualTo("starter");
    assertThat(JwtTenantClaimsNormalizer.plan("   ")).isEqualTo("starter");
  }

  @Test
  void plan_professional_mapsToPro() {
    assertThat(JwtTenantClaimsNormalizer.plan("professional")).isEqualTo("pro");
    assertThat(JwtTenantClaimsNormalizer.plan(" Professional ")).isEqualTo("pro");
  }

  @Test
  void plan_otherValues_trimmedUnchanged() {
    assertThat(JwtTenantClaimsNormalizer.plan("pro")).isEqualTo("pro");
    assertThat(JwtTenantClaimsNormalizer.plan("enterprise")).isEqualTo("enterprise");
  }

  @Test
  void region_nullOrBlank_defaultsToRegionA() {
    assertThat(JwtTenantClaimsNormalizer.region(null)).isEqualTo("region-a");
    assertThat(JwtTenantClaimsNormalizer.region("")).isEqualTo("region-a");
  }

  @Test
  void region_usEast1_mapsToRegionA() {
    assertThat(JwtTenantClaimsNormalizer.region("us-east-1")).isEqualTo("region-a");
    assertThat(JwtTenantClaimsNormalizer.region("US-EAST-1")).isEqualTo("region-a");
  }

  @Test
  void region_otherValues_trimmedUnchanged() {
    assertThat(JwtTenantClaimsNormalizer.region("region-b")).isEqualTo("region-b");
    assertThat(JwtTenantClaimsNormalizer.region("region-a")).isEqualTo("region-a");
  }
}
