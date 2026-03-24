package com.union.solutions.saascore.unit.infrastructure.email;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.union.solutions.saascore.infrastructure.email.SmtpEmailSender;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class SmtpEmailSenderTest {

  @Mock JavaMailSender mailSender;

  private SmtpEmailSender sender;

  @BeforeEach
  void setUp() {
    MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    sender = new SmtpEmailSender(mailSender, "from@test.com", "App", false);
  }

  @Test
  void send_delegatesToMailSender() {
    sender.send("to@test.com", "Subject", "<p>hi</p>");
    verify(mailSender).send(any(MimeMessage.class));
  }

  @Test
  void send_whenFailOnDeliveryTrue_throwsOnMailError() {
    SmtpEmailSender strict = new SmtpEmailSender(mailSender, "from@test.com", "", true);
    doThrow(new MailSendException("smtp down")).when(mailSender).send(any(MimeMessage.class));

    assertThatThrownBy(() -> strict.send("to@test.com", "S", "html"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("SMTP");
  }
}
