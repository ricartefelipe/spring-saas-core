package com.union.solutions.saascore.application.port;

/**
 * Resultado de uma tentativa de envio transacional. Usado para decidir se a senha provisória pode
 * ser mostrada ao admin quando o fornecedor não aceita a mensagem (Resend 403, SMTP bloqueado,
 * etc.).
 */
public record EmailDispatchResult(boolean acceptedByProvider, boolean wasAttempted) {

  /** Resend/SMTP aceitou a mensagem (ex.: HTTP 2xx). */
  public static EmailDispatchResult accepted() {
    return new EmailDispatchResult(true, true);
  }

  /** Modo {@code log}: nenhuma chamada de rede. */
  public static EmailDispatchResult notAttempted() {
    return new EmailDispatchResult(false, false);
  }

  /** Chamada feita mas o fornecedor recusou ou falhou (soft-fail). */
  public static EmailDispatchResult rejectedAfterAttempt() {
    return new EmailDispatchResult(false, true);
  }
}
