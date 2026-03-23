package com.union.solutions.saascore.infrastructure.email;

import com.union.solutions.saascore.application.port.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.email.provider", havingValue = "log", matchIfMissing = true)
public class LogEmailSender implements EmailSender {

  private static final Logger log = LoggerFactory.getLogger(LogEmailSender.class);

  @Override
  public void send(String to, String subject, String htmlBody) {
    log.warn(
        "EMAIL NOT SENT (provider=log). For real delivery set app.email.provider=resend (RESEND_API_KEY) "
            + "or app.email.provider=smtp (SMTP_HOST, SMTP_USER, SMTP_PASSWORD, etc.). [to={}, subject={}]\n"
            + "--- HTML body start ---\n{}\n--- HTML body end ---",
        to,
        subject,
        htmlBody);
  }
}
