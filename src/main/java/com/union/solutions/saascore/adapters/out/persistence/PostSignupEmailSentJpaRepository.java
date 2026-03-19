package com.union.solutions.saascore.adapters.out.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostSignupEmailSentJpaRepository
    extends JpaRepository<PostSignupEmailSentEntity, PostSignupEmailSentEntity.Pk> {

  boolean existsByUserIdAndEmailType(UUID userId, String emailType);
}
