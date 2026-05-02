package com.union.solutions.saascore.adapters.out.persistence;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subscription_trial_reminder_sent")
@IdClass(SubscriptionTrialReminderSentEntity.Pk.class)
public class SubscriptionTrialReminderSentEntity {

  @Id
  @Column(name = "subscription_id", nullable = false)
  private UUID subscriptionId;

  @Id
  @Column(name = "reminder_type", nullable = false, length = 24)
  private String reminderType;

  @Column(name = "sent_at", nullable = false)
  private Instant sentAt;

  public static SubscriptionTrialReminderSentEntity of(UUID subscriptionId, String reminderType) {
    SubscriptionTrialReminderSentEntity e = new SubscriptionTrialReminderSentEntity();
    e.subscriptionId = subscriptionId;
    e.reminderType = reminderType;
    e.sentAt = Instant.now();
    return e;
  }

  @SuppressWarnings("serial")
  public static class Pk implements Serializable {
    private UUID subscriptionId;
    private String reminderType;

    public Pk() {}

    public Pk(UUID subscriptionId, String reminderType) {
      this.subscriptionId = subscriptionId;
      this.reminderType = reminderType;
    }

    public UUID getSubscriptionId() {
      return subscriptionId;
    }

    public void setSubscriptionId(UUID subscriptionId) {
      this.subscriptionId = subscriptionId;
    }

    public String getReminderType() {
      return reminderType;
    }

    public void setReminderType(String reminderType) {
      this.reminderType = reminderType;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Pk pk = (Pk) o;
      return java.util.Objects.equals(subscriptionId, pk.subscriptionId)
          && java.util.Objects.equals(reminderType, pk.reminderType);
    }

    @Override
    public int hashCode() {
      return java.util.Objects.hash(subscriptionId, reminderType);
    }
  }
}
