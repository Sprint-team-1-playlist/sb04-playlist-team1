package com.codeit.playlist.domain.user.entity;

import com.codeit.playlist.domain.base.BaseUpdatableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name ="user_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserToken extends BaseUpdatableEntity {

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "token", nullable = false, length = 512)
  private String token;  // JWT 토큰 (Access 또는 Refresh 구분 없이 저장)

  @Column(name = "issued_at", nullable = false)
  private Instant issuedAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "revoked", nullable = false)
  private Boolean revoked;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  //토큰 종류 구분(Access / Refresh)
  @Column(name = "token_type", nullable = false, length = 10)
  private String tokenType; // ACCESS 또는 REFRESH

  public boolean isExpired() {
    return expiresAt.isBefore(Instant.now());
  }

  public boolean isActive() {
    return !revoked && !isExpired();
  }

  public void revoke() {
    this.revoked = true;
    this.revokedAt = Instant.now();
  }

}
