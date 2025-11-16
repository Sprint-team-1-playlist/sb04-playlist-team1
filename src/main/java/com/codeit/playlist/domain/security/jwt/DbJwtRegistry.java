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
    tokens.forEach(UserToken::revoke);
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
    return token.isPresent() && token.get().isActive() && "ACCESS".equals(
        token.get().getTokenType());
  }

  @Override
  public boolean hasActiveJwtInformationByRefreshToken(String refreshToken) {
    Optional<UserToken> token = userTokenRepository.findByTokenAndRevokedFalse(refreshToken);
    return token.isPresent() && token.get().isActive() && "REFRESH".equals(
        token.get().getTokenType());
  }

  @Transactional
  @Override
  public void rotateJwtInformation(String refreshToken, JwtInformation newJwtInformation) {
    Optional<UserToken> refreshTokenEntityOpt = userTokenRepository.findByTokenAndRevokedFalse(
        refreshToken);

    refreshTokenEntityOpt.ifPresent(refreshTokenEntity -> {

      // 기존 토큰 쌍 무효화
      refreshTokenEntity.revoke();
      userTokenRepository.save(refreshTokenEntity);

      // 같은 userId의 기존 ACCESS 토큰 찾아서 무효화
      List<UserToken> accessTokens = userTokenRepository.findByUserIdAndRevokedFalse(newJwtInformation.userDto().id())
          .stream()
          .filter(token -> "ACCESS".equals(token.getTokenType()))
          .toList();
      accessTokens.forEach(UserToken::revoke);
      userTokenRepository.saveAll(accessTokens);

      // 새로운 토큰 쌍 등록
      registerJwtInformation(newJwtInformation);
    });
  }

  @Transactional
  @Override
  public void clearExpiredJwtInformation() {
    List<UserToken> expiredTokens = userTokenRepository.findExpiredTokens(Instant.now());
      expiredTokens.forEach(UserToken::revoke);
      userTokenRepository.saveAll(expiredTokens);
  }

  @Transactional
  public void revokeRefreshToken(String refreshToken) {
    Optional<UserToken> token = userTokenRepository.findByTokenAndRevokedFalse(refreshToken);
    token.ifPresent(t -> {
      t.revoke(); // revoked = true, revokedAt = now
      userTokenRepository.save(t);
    });
  }

  @Transactional
  public void revokeByToken(String token) {
    userTokenRepository.findByTokenAndRevokedFalse(token)
        .ifPresent(t -> {
          t.revoke();
          userTokenRepository.save(t);
        });
  }

}
