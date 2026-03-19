package com.union.solutions.saascore.unit.application.email;

import static org.assertj.core.api.Assertions.assertThat;

import com.union.solutions.saascore.application.email.EmailTemplates;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class EmailTemplatesTest {

  private static final UUID PLATFORM_TENANT_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID OTHER_TENANT_ID =
      UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

  @Nested
  class InviteEmailDisplayName {

    @Test
    void whenTenantNameIsSystem_bodyShowsFluxeB2BSuite() {
      String html =
          EmailTemplates.inviteEmail(
              OTHER_TENANT_ID, "João", "System", "https://app.example.com/login", "TempPass123");
      assertThat(html).contains(EmailTemplates.PRODUCT_DISPLAY_NAME);
      assertThat(html).doesNotContain(">System<");
    }

    @Test
    void whenPlatformTenant_alwaysFluxeEvenIfDbSaysSystem() {
      String html =
          EmailTemplates.inviteEmail(
              PLATFORM_TENANT_ID, "João", "System", "https://app.example.com/login", "x");
      assertThat(html).contains(EmailTemplates.PRODUCT_DISPLAY_NAME);
      assertThat(html).doesNotContain(">System<");
    }

    @Test
    void whenTenantNameIsSistema_bodyShowsFluxeB2BSuite() {
      String html =
          EmailTemplates.inviteEmail(
              OTHER_TENANT_ID, "Maria", "Sistema", "https://app.example.com/login", "TempPass456");
      assertThat(html).contains(EmailTemplates.PRODUCT_DISPLAY_NAME);
      assertThat(html).doesNotContain(">Sistema<");
    }

    @Test
    void whenTenantNameIsAcme_bodyShowsAcme() {
      String html =
          EmailTemplates.inviteEmail(
              OTHER_TENANT_ID, "Admin", "Acme Distribuidora", "https://app.example.com/login", "x");
      assertThat(html).contains("Acme Distribuidora");
    }

    @Test
    void whenTenantNameIsBlank_bodyShowsFluxeB2BSuite() {
      String html =
          EmailTemplates.inviteEmail(
              OTHER_TENANT_ID, "User", "  ", "https://app.example.com/login", null);
      assertThat(html).contains(EmailTemplates.PRODUCT_DISPLAY_NAME);
    }

    @Test
    void whenTenantNameIsNull_bodyShowsFluxeB2BSuite() {
      String html =
          EmailTemplates.inviteEmail(
              OTHER_TENANT_ID, "User", null, "https://app.example.com/login", null);
      assertThat(html).contains(EmailTemplates.PRODUCT_DISPLAY_NAME);
    }

    @Test
    void inviteEmailWithTempPassword_containsObrigatorioBlock() {
      String html =
          EmailTemplates.inviteEmail(
              PLATFORM_TENANT_ID, "Felipe", "System", "https://app.example.com/login", "Abc123xyz");
      assertThat(html).contains("Obrigatório");
      assertThat(html).contains("deve");
      assertThat(html).contains("nova senha");
      assertThat(html).contains("Abc123xyz");
      assertThat(html).contains(EmailTemplates.PRODUCT_DISPLAY_NAME);
    }
  }

  @Nested
  class ReactivationEmail {

    @Test
    void containsUserNameTenantNameAndLoginUrl() {
      String html =
          EmailTemplates.reactivationEmail("Maria", "Acme Corp", "https://app.example.com/login");
      assertThat(html).contains("Maria");
      assertThat(html).contains("Acme Corp");
      assertThat(html).contains("https://app.example.com/login");
      assertThat(html).contains("Acessar agora");
      assertThat(html).contains(EmailTemplates.PRODUCT_DISPLAY_NAME);
    }

    @Test
    void escapesHtmlInInputs() {
      String html =
          EmailTemplates.reactivationEmail("Maria <script>", "Acme & Co", "https://example.com");
      assertThat(html).doesNotContain("<script>");
      assertThat(html).contains("&amp;");
    }
  }

  @Nested
  class PostSignupDay3Email {

    @Test
    void containsUserNameAndTenantName() {
      String html = EmailTemplates.postSignupDay3Email("João", "Minha Empresa");
      assertThat(html).contains("João");
      assertThat(html).contains("Minha Empresa");
      assertThat(html).contains(EmailTemplates.PRODUCT_DISPLAY_NAME);
    }
  }

  @Nested
  class PostSignupDay7Email {

    @Test
    void containsUserNameAndTenantName() {
      String html = EmailTemplates.postSignupDay7Email("Ana", "Tech Ltda");
      assertThat(html).contains("Ana");
      assertThat(html).contains("Tech Ltda");
      assertThat(html).contains(EmailTemplates.PRODUCT_DISPLAY_NAME);
    }
  }
}
