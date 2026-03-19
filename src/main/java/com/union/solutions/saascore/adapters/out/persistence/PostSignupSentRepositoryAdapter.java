package com.union.solutions.saascore.adapters.out.persistence;

import com.union.solutions.saascore.application.port.PostSignupSentRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PostSignupSentRepositoryAdapter implements PostSignupSentRepository {

  private final PostSignupEmailSentJpaRepository jpa;

  public PostSignupSentRepositoryAdapter(PostSignupEmailSentJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public void recordSent(UUID userId, String emailType) {
    jpa.save(PostSignupEmailSentEntity.of(userId, emailType));
  }

  @Override
  public boolean existsByUserIdAndEmailType(UUID userId, String emailType) {
    return jpa.existsByUserIdAndEmailType(userId, emailType);
  }
}
