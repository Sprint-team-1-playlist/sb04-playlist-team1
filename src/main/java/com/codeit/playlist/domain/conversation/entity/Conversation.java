package com.codeit.playlist.domain.conversation.entity;

import com.codeit.playlist.domain.base.BaseUpdatableEntity;
import com.codeit.playlist.domain.user.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "conversations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Conversation extends BaseUpdatableEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user1_id", nullable = false)
  private User user1;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user2_id", nullable = false)
  private User user2;

  public Conversation(User user1, User user2) {
    this.user1 = user1;
    this.user2 = user2;
  }

  public boolean isParticipant(UUID userId) {
    return user1.getId().equals(userId) || user2.getId().equals(userId);
  }
}
