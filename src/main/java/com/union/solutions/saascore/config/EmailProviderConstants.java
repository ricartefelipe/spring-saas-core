package com.union.solutions.saascore.config;

import java.util.Locale;

/**
 * Normaliza {@code app.email.provider} / {@code EMAIL_PROVIDER} para decisões de envio e beans
 * condicionais. Valores desconhecidos caem em {@code log} para não deixar a app sem {@code EmailSender}.
 */
public final class EmailProviderConstants {

  private EmailProviderConstants() {}

  public static String normalize(String raw) {
    if (raw == null) {
      return "log";
    }
    String t = raw.trim();
    if (t.isEmpty()) {
      return "log";
    }
    String lower = t.toLowerCase(Locale.ROOT);
    if ("resend".equals(lower) || "smtp".equals(lower) || "log".equals(lower)) {
      return lower;
    }
    return "log";
  }
}
