package com.union.solutions.saascore.adapters.out.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PasswordResetTokenJpaRepository
    extends JpaRepository<PasswordResetTokenEntity, UUID> {

  @Query("SELECT t FROM PasswordResetTokenEntity t WHERE t.id = :id AND t.used = false")
  Optional<PasswordResetTokenEntity> findByIdAndNotUsed(UUID id);

  @Modifying
  @Query("DELETE FROM PasswordResetTokenEntity t WHERE t.expiresAt < :cutoff")
  void deleteExpiredBefore(Instant cutoff);
}
