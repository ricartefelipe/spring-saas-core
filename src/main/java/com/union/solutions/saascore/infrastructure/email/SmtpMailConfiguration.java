package com.union.solutions.saascore.infrastructure.email;

import java.util.Properties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * Configura {@link JavaMailSender} quando {@code app.email.provider=smtp}. Usa host/porta/credenciais
 * próprios (Gmail app password, SendGrid SMTP, SES, Postfix, etc.).
 */
@Configuration
@ConditionalOnProperty(name = "app.email.provider", havingValue = "smtp")
public class SmtpMailConfiguration {

  @Bean
  public JavaMailSender javaMailSender(
      @Value("${app.email.smtp.host:}") String host,
      @Value("${app.email.smtp.port:587}") int port,
      @Value("${app.email.smtp.username:}") String username,
      @Value("${app.email.smtp.password:}") String password,
      @Value("${app.email.smtp.auth:true}") boolean auth,
      @Value("${app.email.smtp.starttls:true}") boolean startTls) {
    if (host == null || host.isBlank()) {
      throw new IllegalStateException(
          "app.email.provider=smtp requires app.email.smtp.host (e.g. SMTP_HOST). "
              + "See docs/CONVITE-EMAIL-DEPLOY.md.");
    }
    JavaMailSenderImpl sender = new JavaMailSenderImpl();
    sender.setHost(host.trim());
    sender.setPort(port);
    if (username != null && !username.isBlank()) {
      sender.setUsername(username.trim());
    }
    if (password != null && !password.isBlank()) {
      sender.setPassword(password);
    }
    Properties props = sender.getJavaMailProperties();
    props.put("mail.transport.protocol", "smtp");
    props.put("mail.smtp.auth", Boolean.toString(auth));
    props.put("mail.smtp.starttls.enable", Boolean.toString(startTls));
    props.put("mail.smtp.starttls.required", Boolean.toString(startTls));
    props.put("mail.debug", "false");
    return sender;
  }
}
