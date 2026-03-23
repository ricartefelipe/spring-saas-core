package com.union.solutions.saascore.application.user;

import com.union.solutions.saascore.domain.User;
import java.util.Optional;

/** Result of {@link UserManagementUseCase#invite}; password is only for API when email is not sent. */
public record InviteOutcome(User user, Optional<String> temporaryPasswordForResponse) {

  public static InviteOutcome withPasswordForLog(User user, String plainPassword) {
    return new InviteOutcome(user, Optional.of(plainPassword));
  }

  public static InviteOutcome withoutPassword(User user) {
    return new InviteOutcome(user, Optional.empty());
  }
}
