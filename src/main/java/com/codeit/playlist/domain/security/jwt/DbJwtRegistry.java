package com.codeit.playlist.domain.security.jwt;

import com.codeit.playlist.domain.user.entity.UserToken;
import com.codeit.playlist.domain.user.repository.UserTokenRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DbJwtRegistry implements JwtRegistry {

  private final UserTokenRepository userTokenRepository;

  @Transactional
  @Override
  public void registerJwtInformation(JwtInformation jwtInformation) {
    // Access 토큰 저장
    UserToken accessTokenEntity = UserToken.builder()
        .userId(jwtInformation.userDto().id())
        .token(jwtInformation.accessToken())
        .tokenType("ACCESS")
        .issuedAt(Instant.now())
        .expiresAt(jwtInformation.accessTokenExpiresAt()) // JwtInformation 에서 만료시간 받아야함
        .revoked(false)
        .build();

    userTokenRepository.save(accessTokenEntity);

    // Refresh 토큰 저장
    UserToken refreshTokenEntity = UserToken.builder()
        .userId(jwtInformation.userDto().id())
        .token(jwtInformation.refreshToken())
        .tokenType("REFRESH")
        .issuedAt(Instant.now())
        .expiresAt(jwtInformation.refreshTokenExpiresAt()) // JwtInformation 에서 만료시간 받아야함
        .revoked(false)
        .build();

    userTokenRepository.save(refreshTokenEntity);
  }

  @Transactional
  @Override
  public void invalidateJwtInformationByUserId(UUID userId) {
    List<UserToken> tokens = userTokenRepository.findByUserIdAndRevokedFalse(userId);
    tokens.forEach(token -> {
      token.setRevoked(true);
      token.setRevokedAt(Instant.now());
    });
    userTokenRepository.saveAll(tokens);
  }

  @Override
  public boolean hasActiveJwtInformationByUserId(UUID userId) {
    List<UserToken> tokens = userTokenRepository.findByUserIdAndRevokedFalse(userId);
    return tokens.stream().anyMatch(UserToken::isActive);
  }

  @Override
  public boolean hasActiveJwtInformationByAccessToken(String accessToken) {
    Optional<UserToken> token = userTokenRepository.findByTokenAndRevokedFalse(accessToken);
    return token.isPresent() && token.get().isActive() && "ACCESS".equals(token.get().getTokenType());
  }

  @Override
  public boolean hasActiveJwtInformationByRefreshToken(String refreshToken) {
    Optional<UserToken> token = userTokenRepository.findByTokenAndRevokedFalse(refreshToken);
    return token.isPresent() && token.get().isActive() && "REFRESH".equals(token.get().getTokenType());
  }

  @Transactional
  @Override
  public void rotateJwtInformation(String refreshToken, JwtInformation newJwtInformation) {
    Optional<UserToken> refreshTokenEntityOpt = userTokenRepository.findByTokenAndRevokedFalse(refreshToken);

    refreshTokenEntityOpt.ifPresent(refreshTokenEntity -> {
      // Refresh 토큰 업데이트
      refreshTokenEntity.setToken(newJwtInformation.refreshToken());
      refreshTokenEntity.setExpiresAt(newJwtInformation.refreshTokenExpiresAt());

      userTokenRepository.save(refreshTokenEntity);

      // 대응하는 Access 토큰도 업데이트
      List<UserToken> accessTokens = userTokenRepository.findByUserIdAndRevokedFalse(newJwtInformation.userDto().id());
      accessTokens.stream()
          .filter(token -> "ACCESS".equals(token.getTokenType()))
          .forEach(accessTokenEntity -> {
            accessTokenEntity.setToken(newJwtInformation.accessToken());
            accessTokenEntity.setExpiresAt(newJwtInformation.accessTokenExpiresAt());
          });
      userTokenRepository.saveAll(accessTokens);
    });
  }

  @Transactional
  @Override
  public void clearExpiredJwtInformation() {
    List<UserToken> tokens = userTokenRepository.findAll();

    tokens.stream()
        .filter(token -> token.isExpired())
        .forEach(token -> {
          token.setRevoked(true);
          token.setRevokedAt(Instant.now());
          userTokenRepository.save(token);
        });
  }

  public void revokeRefreshToken(String refreshToken) {
    Optional<UserToken> token = userTokenRepository.findByTokenAndRevokedFalse(refreshToken);
    token.ifPresent(t -> {
      t.revoke(); // revoked = true, revokedAt = now
      userTokenRepository.save(t);
    });
  }

  public void revokeByToken(String token) {
    userTokenRepository.findByTokenAndRevokedFalse(token)
        .ifPresent(UserToken::revoke);
  }

}
