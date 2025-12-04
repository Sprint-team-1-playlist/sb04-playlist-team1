package com.codeit.playlist.domain.user.entity;

import com.codeit.playlist.domain.base.BaseUpdatableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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
  private boolean locked;

  @Column(name = "follow_count", nullable = false)
  private Long followCount;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AuthProvider provider = AuthProvider.LOCAL;

  public User(String email, String password, String name, String profileImageUrl, Role role) {
    this.email = email;
    this.password = password;
    this.name = name;
    this.profileImageUrl = profileImageUrl;
    this.role = role;
    this.locked = false;
    this.followCount = 0L;
    this.provider = AuthProvider.LOCAL;
  }

  public void increaseFollowCount() {
    if (followCount == null) {
      followCount = 0L;
    }
    followCount++;
  }

  public void decreaseFollowCount() {
    if (followCount == null || followCount <= 0) {
      followCount = 0L;
    } else {
      followCount--;
    }
  }

  public void updateUsername(String name) {
    this.name = name;
  }

  public void updateProfileImageUrl(String profileImageUrl) {
    this.profileImageUrl = profileImageUrl;
  }


  public void updatePassword(String password) {
    this.password = password;
  }

  public void updateRole(Role newRole) {
    if (this.role != newRole) {
      this.role = newRole;
    }
  }

  public void setLocked(boolean locked) {
    this.locked = locked;
  }

  // Oauth 소셜 로그인을 위함 정적 팩토리 메서드
  public static User createOAuthUser(String name, String email,  String imageUrl, AuthProvider provider) {
    User user = new User();
    user.name = name;
    user.email = email;
    user.profileImageUrl = imageUrl;
    user.password = java.util.UUID.randomUUID().toString();      // 소셜 유저는 로그인 불가능한 랜덤 패스워드
    user.provider = provider;
    user.role = Role.USER;
    user.locked = false;
    user.followCount = 0L;
    return user;
  }
}
