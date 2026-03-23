package com.union.solutions.saascore.infrastructure.email;

import com.union.solutions.saascore.application.port.EmailSender;
import com.union.solutions.saascore.config.ConditionalOnEmailProvider;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnEmailProvider("smtp")
public class SmtpEmailSender implements EmailSender {

  private static final Logger log = LoggerFactory.getLogger(SmtpEmailSender.class);

  private final JavaMailSender mailSender;
  private final String rawFrom;
  private final String fromName;
  private final boolean failOnDeliveryError;

  public SmtpEmailSender(
      JavaMailSender mailSender,
      @Value("${app.email.from:noreply@fluxe.com.br}") String fromAddress,
      @Value("${app.email.from-name:}") String fromName,
      @Value("${app.email.fail-on-delivery-error:false}") boolean failOnDeliveryError) {
    this.mailSender = mailSender;
    this.rawFrom =
        fromAddress != null && !fromAddress.isBlank() ? fromAddress.trim() : "noreply@fluxe.com.br";
    this.fromName = fromName != null ? fromName.trim() : "";
    this.failOnDeliveryError = failOnDeliveryError;
  }

  @Override
  public void send(String to, String subject, String htmlBody) {
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
      helper.setFrom(buildFrom());
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(htmlBody, true);
      mailSender.send(message);
      log.info("Email sent via SMTP to={} subject={}", to, subject);
    } catch (MessagingException | MailException e) {
      String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
      log.error(
          "SMTP email delivery failed. to={} subject={} error={}. "
              + "Check host/port/auth, firewall, and that FROM is allowed by the provider.",
          to,
          subject,
          msg);
      if (!failOnDeliveryError) {
        log.warn(
            "SMTP falhou; destinatário pode não ter recebido (fail-on-delivery-error=false). HTML:\n{}",
            htmlBody);
        return;
      }
      throw new IllegalStateException(
          "Email delivery failed via SMTP. Check app.email.smtp.* and provider limits. Details: " + msg,
          e);
    }
  }

  private InternetAddress buildFrom() {
    try {
      if (fromName.isEmpty()) {
        return new InternetAddress(rawFrom);
      }
      return new InternetAddress(rawFrom, fromName, StandardCharsets.UTF_8.name());
    } catch (MessagingException | UnsupportedEncodingException e) {
      throw new IllegalStateException("Invalid app.email.from / from-name for SMTP", e);
    }
  }
}
