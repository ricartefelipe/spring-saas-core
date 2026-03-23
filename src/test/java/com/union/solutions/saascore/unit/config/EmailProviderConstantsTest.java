package com.union.solutions.saascore.unit.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.union.solutions.saascore.config.EmailProviderConstants;
import org.junit.jupiter.api.Test;

class EmailProviderConstantsTest {

  @Test
  void normalize_uppercaseSmtp_becomesSmtp() {
    assertThat(EmailProviderConstants.normalize("SMTP")).isEqualTo("smtp");
  }

  @Test
  void normalize_uppercaseResend_becomesResend() {
    assertThat(EmailProviderConstants.normalize("RESEND")).isEqualTo("resend");
  }

  @Test
  void normalize_blank_isLog() {
    assertThat(EmailProviderConstants.normalize("")).isEqualTo("log");
    assertThat(EmailProviderConstants.normalize(null)).isEqualTo("log");
  }

  @Test
  void normalize_unknown_defaultsToLog() {
    assertThat(EmailProviderConstants.normalize("imap")).isEqualTo("log");
  }
}
