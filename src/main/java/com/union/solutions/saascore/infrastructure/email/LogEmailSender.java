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
    log.info(
        "EMAIL [to={}, subject={}]\n--- HTML body start ---\n{}\n--- HTML body end ---",
        to,
        subject,
        htmlBody);
  }
}
