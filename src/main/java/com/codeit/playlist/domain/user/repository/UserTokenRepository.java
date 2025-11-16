package com.codeit.playlist.domain.user.repository;

import com.codeit.playlist.domain.user.entity.UserToken;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTokenRepository extends JpaRepository<UserToken, UUID> {

  List<UserToken> findByUserIdAndRevokedFalse(UUID userId);

  Optional<UserToken> findByTokenAndRevokedFalse(String token);

  void deleteByUserId(UUID userId);

}
