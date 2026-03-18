package com.union.solutions.saascore.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class User {

  private UUID id;
  private String email;
  private String name;
  private String passwordHash;
  private UUID tenantId;
  private List<String> roles;
  private UserStatus status;
  private boolean mustChangePassword;
  private Instant createdAt;
  private Instant updatedAt;

  public User() {}

  public User(
      UUID id,
      String email,
      String name,
      String passwordHash,
      UUID tenantId,
      List<String> roles,
      UserStatus status,
      Instant createdAt,
      Instant updatedAt) {
    this(id, email, name, passwordHash, tenantId, roles, status, false, createdAt, updatedAt);
  }

  public User(
      UUID id,
      String email,
      String name,
      String passwordHash,
      UUID tenantId,
      List<String> roles,
      UserStatus status,
      boolean mustChangePassword,
      Instant createdAt,
      Instant updatedAt) {
    this.id = id;
    this.email = email;
    this.name = name;
    this.passwordHash = passwordHash;
    this.tenantId = tenantId;
    this.roles = roles != null ? roles : List.of();
    this.status = status;
    this.mustChangePassword = mustChangePassword;
    this.createdAt = createdAt != null ? createdAt : Instant.now();
    this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
  }

  public boolean isMustChangePassword() {
    return mustChangePassword;
  }

  public void setMustChangePassword(boolean mustChangePassword) {
    this.mustChangePassword = mustChangePassword;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public void setTenantId(UUID tenantId) {
    this.tenantId = tenantId;
  }

  public List<String> getRoles() {
    return roles;
  }

  public void setRoles(List<String> roles) {
    this.roles = roles;
  }

  public UserStatus getStatus() {
    return status;
  }

  public void setStatus(UserStatus status) {
    this.status = status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  public boolean isActive() {
    return status == UserStatus.ACTIVE;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof User that)) return false;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  public enum UserStatus {
    PENDING,
    ACTIVE,
    SUSPENDED,
    DELETED
  }
}
