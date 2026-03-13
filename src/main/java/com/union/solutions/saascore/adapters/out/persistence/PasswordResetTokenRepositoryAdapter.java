package com.union.solutions.saascore.adapters.out.persistence;

import com.union.solutions.saascore.application.port.PasswordResetTokenRepository;
import com.union.solutions.saascore.domain.PasswordResetToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PasswordResetTokenRepositoryAdapter implements PasswordResetTokenRepository {

  private final PasswordResetTokenJpaRepository jpa;

  public PasswordResetTokenRepositoryAdapter(PasswordResetTokenJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public PasswordResetToken save(PasswordResetToken token) {
    return jpa.save(PasswordResetTokenEntity.from(token)).toDomain();
  }

  @Override
  public Optional<PasswordResetToken> findByIdAndNotUsed(UUID id) {
    return jpa.findByIdAndNotUsed(id).map(PasswordResetTokenEntity::toDomain);
  }

  @Override
  public void deleteExpiredBefore(Instant cutoff) {
    jpa.deleteExpiredBefore(cutoff);
  }
}
