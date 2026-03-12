package com.union.solutions.saascore.application.user;

import java.util.UUID;

public class UserAlreadyExistsException extends RuntimeException {

  private final String email;
  private final UUID tenantId;

  public UserAlreadyExistsException(String email, UUID tenantId) {
    super("User with email '" + email + "' already exists in tenant " + tenantId);
    this.email = email;
    this.tenantId = tenantId;
  }

  public String getEmail() {
    return email;
  }

  public UUID getTenantId() {
    return tenantId;
  }
}
