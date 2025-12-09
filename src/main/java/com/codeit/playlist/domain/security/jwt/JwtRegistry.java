package com.codeit.playlist.domain.security.jwt;

import com.codeit.playlist.domain.user.entity.User;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public interface JwtRegistry {


  @Transactional
  void registerJwtInformation(JwtInformation jwtInformation);

  @Transactional
  void invalidateJwtInformationByUserId(UUID userId);

  boolean hasActiveJwtInformationByUserId(UUID userId);

  boolean hasActiveJwtInformationByAccessToken(String accessToken);

  boolean hasActiveJwtInformationByRefreshToken(String refreshToken);

  @Transactional
  boolean rotateJwtInformation(String oldRefreshToken, JwtInformation newInfo);

  @Transactional
  void clearExpiredJwtInformation();

  void revokeRefreshToken(String refreshToken);

  void revokeByToken(String token);

  JwtTokens issueNewTokensAndInvalidateOld(User user);
}
