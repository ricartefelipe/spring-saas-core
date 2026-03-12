package com.union.solutions.saascore.application.port;

public interface EmailSender {

  void send(String to, String subject, String htmlBody);
}
