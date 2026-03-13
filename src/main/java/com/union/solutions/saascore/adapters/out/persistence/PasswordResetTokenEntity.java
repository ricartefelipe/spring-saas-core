package com.union.solutions.saascore.adapters.out.persistence;

import com.union.solutions.saascore.domain.PasswordResetToken;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetTokenEntity {

  @Id
  @Column(name = "id")
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "token_hash", nullable = false)
  private String tokenHash;

  @Column(name = "used", nullable = false)
  private boolean used;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  public static PasswordResetTokenEntity from(PasswordResetToken t) {
    PasswordResetTokenEntity e = new PasswordResetTokenEntity();
    e.id = t.getId();
    e.userId = t.getUserId();
    e.tokenHash = t.getTokenHash();
    e.used = t.isUsed();
    e.expiresAt = t.getExpiresAt();
    e.createdAt = t.getCreatedAt();
    return e;
  }

  public PasswordResetToken toDomain() {
    return new PasswordResetToken(id, userId, tokenHash, used, expiresAt, createdAt);
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public String getTokenHash() {
    return tokenHash;
  }

  public void setTokenHash(String tokenHash) {
    this.tokenHash = tokenHash;
  }

  public boolean isUsed() {
    return used;
  }

  public void setUsed(boolean used) {
    this.used = used;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
