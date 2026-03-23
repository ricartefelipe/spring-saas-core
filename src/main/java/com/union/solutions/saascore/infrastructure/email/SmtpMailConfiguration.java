package com.union.solutions.saascore.infrastructure.email;

import com.union.solutions.saascore.config.ConditionalOnEmailProvider;
import java.util.Properties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * Configura {@link JavaMailSender} quando {@code app.email.provider=smtp}. Usa host/porta/credenciais
 * próprios (Gmail app password, SendGrid SMTP, SES, Postfix, etc.).
 *
 * <p>Porta 465 costuma exigir SSL implícito (não STARTTLS); isso é aplicado automaticamente ou via
 * {@code app.email.smtp.ssl.enabled}.
 */
@Configuration
@ConditionalOnEmailProvider("smtp")
public class SmtpMailConfiguration {

  @Bean
  @Primary
  public JavaMailSender javaMailSender(
      @Value("${app.email.smtp.host:}") String host,
      @Value("${app.email.smtp.port:587}") int port,
      @Value("${app.email.smtp.username:}") String username,
      @Value("${app.email.smtp.password:}") String password,
      @Value("${app.email.smtp.auth:true}") boolean auth,
      @Value("${app.email.smtp.starttls:true}") boolean startTls,
      @Value("${app.email.smtp.ssl.enabled:false}") boolean sslEnabled) {
    if (host == null || host.isBlank()) {
      throw new IllegalStateException(
          "app.email.provider=smtp requires app.email.smtp.host (e.g. SMTP_HOST). "
              + "See docs/CONVITE-EMAIL-DEPLOY.md.");
    }
    boolean implicitSsl = sslEnabled || port == 465;

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
    props.put("mail.smtp.connectiontimeout", "30000");
    props.put("mail.smtp.timeout", "30000");
    props.put("mail.smtp.writetimeout", "30000");

    if (implicitSsl) {
      props.put("mail.smtp.ssl.enable", "true");
      props.put("mail.smtp.socketFactory.port", String.valueOf(port));
      props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
      props.put("mail.smtp.socketFactory.fallback", "false");
      props.put("mail.smtp.starttls.enable", "false");
      props.put("mail.smtp.starttls.required", "false");
    } else {
      props.put("mail.smtp.starttls.enable", Boolean.toString(startTls));
      props.put("mail.smtp.starttls.required", Boolean.toString(startTls));
    }
    props.put("mail.debug", "false");
    return sender;
  }
}
