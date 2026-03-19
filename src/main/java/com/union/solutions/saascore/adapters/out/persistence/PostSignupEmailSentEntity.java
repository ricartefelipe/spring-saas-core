package com.union.solutions.saascore.adapters.out.persistence;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "post_signup_email_sent")
@IdClass(PostSignupEmailSentEntity.Pk.class)
public class PostSignupEmailSentEntity {

  @Id
  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Id
  @Column(name = "email_type", nullable = false, length = 16)
  private String emailType;

  @Column(name = "sent_at", nullable = false)
  private Instant sentAt;

  public static PostSignupEmailSentEntity of(UUID userId, String emailType) {
    PostSignupEmailSentEntity e = new PostSignupEmailSentEntity();
    e.userId = userId;
    e.emailType = emailType;
    e.sentAt = Instant.now();
    return e;
  }

  public UUID getUserId() {
    return userId;
  }

  public String getEmailType() {
    return emailType;
  }

  public Instant getSentAt() {
    return sentAt;
  }

  @SuppressWarnings("serial")
  public static class Pk implements Serializable {
    private UUID userId;
    private String emailType;

    public Pk() {}

    public Pk(UUID userId, String emailType) {
      this.userId = userId;
      this.emailType = emailType;
    }

    public UUID getUserId() {
      return userId;
    }

    public void setUserId(UUID userId) {
      this.userId = userId;
    }

    public String getEmailType() {
      return emailType;
    }

    public void setEmailType(String emailType) {
      this.emailType = emailType;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Pk pk = (Pk) o;
      return java.util.Objects.equals(userId, pk.userId)
          && java.util.Objects.equals(emailType, pk.emailType);
    }

    @Override
    public int hashCode() {
      return java.util.Objects.hash(userId, emailType);
    }
  }
}
