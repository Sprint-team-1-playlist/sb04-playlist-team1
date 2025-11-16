package com.codeit.playlist.domain.security.jwt;

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
  void rotateJwtInformation(String refreshToken, JwtInformation newJwtInformation);

  @Transactional
  void clearExpiredJwtInformation();

  void revokeRefreshToken(String refreshToken);

  void revokeByToken(String token);

}

