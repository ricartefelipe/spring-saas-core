package com.union.solutions.saascore.unit.application.email;

import static org.assertj.core.api.Assertions.assertThat;

import com.union.solutions.saascore.application.email.EmailTemplates;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class EmailTemplatesTest {

  @Nested
  class InviteEmailDisplayName {

    @Test
    void whenTenantNameIsSystem_bodyShowsFluxeB2BSuite() {
      String html =
          EmailTemplates.inviteEmail(
              "João", "System", "https://app.example.com/login", "TempPass123");
      assertThat(html).contains(EmailTemplates.PRODUCT_DISPLAY_NAME);
      assertThat(html).doesNotContain(">System<");
    }

    @Test
    void whenTenantNameIsSistema_bodyShowsFluxeB2BSuite() {
      String html =
          EmailTemplates.inviteEmail(
              "Maria", "Sistema", "https://app.example.com/login", "TempPass456");
      assertThat(html).contains(EmailTemplates.PRODUCT_DISPLAY_NAME);
      assertThat(html).doesNotContain(">Sistema<");
    }

    @Test
    void whenTenantNameIsAcme_bodyShowsAcme() {
      String html =
          EmailTemplates.inviteEmail(
              "Admin", "Acme Distribuidora", "https://app.example.com/login", "x");
      assertThat(html).contains("Acme Distribuidora");
    }

    @Test
    void whenTenantNameIsBlank_bodyShowsFluxeB2BSuite() {
      String html = EmailTemplates.inviteEmail("User", "  ", "https://app.example.com/login", null);
      assertThat(html).contains(EmailTemplates.PRODUCT_DISPLAY_NAME);
    }

    @Test
    void whenTenantNameIsNull_bodyShowsFluxeB2BSuite() {
      String html = EmailTemplates.inviteEmail("User", null, "https://app.example.com/login", null);
      assertThat(html).contains(EmailTemplates.PRODUCT_DISPLAY_NAME);
    }

    @Test
    void inviteEmailWithTempPassword_containsObrigatorioBlock() {
      String html =
          EmailTemplates.inviteEmail(
              "Felipe", "System", "https://app.example.com/login", "Abc123xyz");
      assertThat(html).contains("Obrigatório");
      assertThat(html).contains("deve");
      assertThat(html).contains("nova senha");
      assertThat(html).contains("Abc123xyz");
    }
  }
}
