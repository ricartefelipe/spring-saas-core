package com.union.solutions.saascore.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class User {

  private UUID id;
  private UUID tenantId;
  private String name;
  private String email;
  private List<String> roles;
  private UserStatus status;
  private Instant createdAt;
  private Instant updatedAt;

  public User() {}

  public User(
      UUID id,
      UUID tenantId,
      String name,
      String email,
      List<String> roles,
      UserStatus status,
      Instant createdAt,
      Instant updatedAt) {
    this.id = id;
    this.tenantId = tenantId;
    this.name = name;
    this.email = email;
    this.roles = roles != null ? roles : List.of();
    this.status = status;
    this.createdAt = createdAt != null ? createdAt : Instant.now();
    this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public void setTenantId(UUID tenantId) {
    this.tenantId = tenantId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
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
    ACTIVE,
    PENDING,
    SUSPENDED,
    DELETED
  }
}
