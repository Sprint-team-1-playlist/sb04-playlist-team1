package com.codeit.playlist.domain.user.entity;

import com.codeit.playlist.domain.base.BaseUpdatableEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class User extends BaseUpdatableEntity {

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false, length = 100)
  private String password;

  @Column (nullable = false)
  private String name;

  @Column(name = "profile_image_url")
  private String profileImageUrl;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private Role role = Role.USER;

  @Column(name = "is_locked", nullable = false)
  private boolean isLocked = false;

  @Column(name = "follow_count")
  private Long followCount;

  // 여기부터 연관관계, 초반 설계단계 에러 방지를 위해 주석처리, 각 개발 과정에서 필요한부분 주석 제거하여 사용할것

  //  @OneToMany(mappedBy = "user")
  //  private List<UserToken> tokens;

  // reviews (1:N)
  //  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  //  private List<Review> reviews = new ArrayList<>();

  // subscribes (1:N)
  //  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  //  private List<Subscribe> subscriptions = new ArrayList<>();

  // notifications (1:N)
  //  @OneToMany(mappedBy = "receiver", cascade = CascadeType.ALL, orphanRemoval = true)
  //  private List<Notification> notifications = new ArrayList<>();

  // play_lists (1:N)
  //  @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
  //  private List<PlayList> playLists = new ArrayList<>();

  // follows (1:N) — 팔로워 / 팔로잉
  //  @OneToMany(mappedBy = "follower", cascade = CascadeType.ALL, orphanRemoval = true)
  //  private List<Follow> followings = new ArrayList<>();

  //  @OneToMany(mappedBy = "following", cascade = CascadeType.ALL, orphanRemoval = true)
  //  private List<Follow> followers = new ArrayList<>();

  // direct_messages (1:N, sender_id / receiver_id 두 개)
  //  @OneToMany(mappedBy = "sender", cascade = CascadeType.ALL, orphanRemoval = true)
  //  private List<DirectMessage> sentMessages = new ArrayList<>();

  //  @OneToMany(mappedBy = "receiver", cascade = CascadeType.ALL, orphanRemoval = true)
  //  private List<DirectMessage> receivedMessages = new ArrayList<>();

  public void updatePassword(String password) {
    this.password = password;
  }

  public void updateRole(Role newRole) {
    if (this.role != newRole) {
      this.role = newRole;
    }
  }

}
