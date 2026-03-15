package com.union.solutions.saascore.unit.infrastructure.email;

import static org.assertj.core.api.Assertions.assertThatCode;

import com.union.solutions.saascore.infrastructure.email.LogEmailSender;
import org.junit.jupiter.api.Test;

class LogEmailSenderTest {

  @Test
  void send_doesNotThrow() {
    LogEmailSender sender = new LogEmailSender();
    assertThatCode(() -> sender.send("test@example.com", "Subject", "<p>Body</p>"))
        .doesNotThrowAnyException();
  }

  @Test
  void send_withEmptySubject_doesNotThrow() {
    LogEmailSender sender = new LogEmailSender();
    assertThatCode(() -> sender.send("a@b.com", "", "html")).doesNotThrowAnyException();
  }
}
