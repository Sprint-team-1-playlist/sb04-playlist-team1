package com.codeit.playlist.domain.user.repository;

import com.codeit.playlist.domain.user.entity.UserToken;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserTokenRepository extends JpaRepository<UserToken, UUID> {

  List<UserToken> findByUserIdAndRevokedFalse(UUID userId);

  Optional<UserToken> findByTokenAndRevokedFalse(String token);

  void deleteByUserId(UUID userId);

  @Query("SELECT t FROM UserToken t WHERE t.revoked = false AND t.expiresAt < :now")
  List<UserToken> findExpiredTokens(Instant now);

}
