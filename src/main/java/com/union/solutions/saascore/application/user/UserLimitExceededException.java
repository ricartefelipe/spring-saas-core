package com.union.solutions.saascore.application.user;

import java.util.UUID;

public class UserLimitExceededException extends RuntimeException {

  public UserLimitExceededException(UUID tenantId, String planSlug, int maxUsers) {
    super(
        "User limit reached for tenant "
            + tenantId
            + " on plan "
            + planSlug
            + " (maxUsers="
            + maxUsers
            + ")");
  }
}
