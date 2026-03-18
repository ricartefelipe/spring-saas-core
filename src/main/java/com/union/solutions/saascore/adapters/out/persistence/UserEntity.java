package com.union.solutions.saascore.adapters.out.persistence;

import com.union.solutions.saascore.domain.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserEntity {

  @Id
  @Column(name = "id")
  private UUID id;

  @Column(name = "email", nullable = false, unique = true)
  private String email;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Column(name = "tenant_id", nullable = false)
  private UUID tenantId;

  @Column(name = "roles", nullable = false)
  private String roles;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private User.UserStatus status;

  @Column(name = "must_change_password", nullable = false)
  private boolean mustChangePassword;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public static UserEntity from(User u) {
    UserEntity e = new UserEntity();
    e.id = u.getId();
    e.email = u.getEmail();
    e.name = u.getName();
    e.passwordHash = u.getPasswordHash();
    e.tenantId = u.getTenantId();
    e.roles = u.getRoles() != null ? String.join(",", u.getRoles()) : "";
    e.status = u.getStatus();
    e.mustChangePassword = u.isMustChangePassword();
    e.createdAt = u.getCreatedAt();
    e.updatedAt = u.getUpdatedAt();
    return e;
  }

  public User toDomain() {
    List<String> roleList =
        (roles == null || roles.isBlank()) ? List.of() : Arrays.asList(roles.split(","));
    return new User(
        id,
        email,
        name,
        passwordHash,
        tenantId,
        roleList,
        status,
        mustChangePassword,
        createdAt,
        updatedAt);
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

  public String getRoles() {
    return roles;
  }

  public void setRoles(String roles) {
    this.roles = roles;
  }

  public User.UserStatus getStatus() {
    return status;
  }

  public void setStatus(User.UserStatus status) {
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

  public boolean isMustChangePassword() {
    return mustChangePassword;
  }

  public void setMustChangePassword(boolean mustChangePassword) {
    this.mustChangePassword = mustChangePassword;
  }
}
