package com.union.solutions.saascore.application.port;

public interface EmailSender {

  /**
   * Envia HTML. Chamadas que não precisam do resultado podem ignorar o retorno.
   *
   * @return {@link EmailDispatchResult#accepted()} apenas quando o fornecedor aceitou o envio.
   */
  EmailDispatchResult send(String to, String subject, String htmlBody);
}
