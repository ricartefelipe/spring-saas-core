package com.union.solutions.saascore.application.user;

import java.util.Optional;

/** Result of {@link UserManagementUseCase#resendInvite}; password only when email is not sent (log). */
public record ResendInviteOutcome(Optional<String> temporaryPasswordForResponse) {

  public static ResendInviteOutcome withPasswordForLog(String plainPassword) {
    return new ResendInviteOutcome(Optional.of(plainPassword));
  }

  public static ResendInviteOutcome withoutPassword() {
    return new ResendInviteOutcome(Optional.empty());
  }
}
