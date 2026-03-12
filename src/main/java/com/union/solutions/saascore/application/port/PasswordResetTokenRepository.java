package com.union.solutions.saascore.application.port;

import com.union.solutions.saascore.domain.PasswordResetToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository {

    PasswordResetToken save(PasswordResetToken token);

    Optional<PasswordResetToken> findByIdAndNotUsed(UUID id);

    void deleteExpiredBefore(Instant cutoff);
}
