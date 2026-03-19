package com.union.solutions.saascore.application.port;

import java.util.UUID;

public interface PostSignupSentRepository {

  void recordSent(UUID userId, String emailType);

  boolean existsByUserIdAndEmailType(UUID userId, String emailType);
}
