package com.codeit.playlist.domain.security.jwt;

import com.codeit.playlist.domain.security.PlaylistUserDetails;
import com.codeit.playlist.domain.user.dto.data.UserDto;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.entity.UserToken;
import com.codeit.playlist.domain.user.exception.AuthenticationOAuthJwtException;
import com.codeit.playlist.domain.user.mapper.UserMapper;
import com.codeit.playlist.domain.user.repository.UserTokenRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DbJwtRegistry implements JwtRegistry {

  private final UserTokenRepository userTokenRepository;
  private final UserMapper userMapper;
  private final JwtTokenProvider tokenProvider;

  @Transactional
  @Override
  public void registerJwtInformation(JwtInformation jwtInformation) {
    // Access 토큰 저장
    UserToken accessTokenEntity = UserToken.builder()
        .userId(jwtInformation.userDto().id())
        .token(jwtInformation.accessToken())
        .tokenType("ACCESS")
        .issuedAt(jwtInformation.accessTokenIssuedAt())
        .expiresAt(jwtInformation.accessTokenExpiresAt())
        .revoked(false)
        .build();

    userTokenRepository.save(accessTokenEntity);

    // Refresh 토큰 저장
    UserToken refreshTokenEntity = UserToken.builder()
        .userId(jwtInformation.userDto().id())
        .token(jwtInformation.refreshToken())
        .tokenType("REFRESH")
        .issuedAt(jwtInformation.refreshTokenIssuedAt())
        .expiresAt(jwtInformation.refreshTokenExpiresAt())
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
  public boolean rotateJwtInformation(String oldRefreshToken, JwtInformation newInfo) {
    Optional<UserToken> refreshTokenEntityOpt = userTokenRepository.findByTokenAndRevokedFalse(
        oldRefreshToken);

    // 토큰이 없거나 이미 revoke 되었으면 false 반환
    if (refreshTokenEntityOpt.isEmpty()) {
      return false;
    }

    UserToken refreshTokenEntity = refreshTokenEntityOpt.get();

    // 1. 기존 REFRESH 토큰 무효화
    refreshTokenEntity.revoke();
    userTokenRepository.save(refreshTokenEntity);

    // 2. 기존 ACCESS 토큰 전체 무효화
    List<UserToken> accessTokens = userTokenRepository
        .findByUserIdAndRevokedFalse(newInfo.userDto().id())
        .stream()
        .filter(token -> "ACCESS".equals(token.getTokenType()))
        .toList();

    accessTokens.forEach(UserToken::revoke);
    userTokenRepository.saveAll(accessTokens);

    // 3. 새 토큰 등록
    registerJwtInformation(newInfo);

    return true;
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


  @Transactional
  public JwtTokens issueNewTokensAndInvalidateOld(User user) {
    try {
      // 1. 기존 토큰 무효화 (동시 로그인 방지)
      invalidateJwtInformationByUserId(user.getId());

      // 2. UserDto 변환
      UserDto userDto = userMapper.toDto(user);
      PlaylistUserDetails userDetails = new PlaylistUserDetails(userDto, null);

      // 3. 새 토큰 생성
      Instant now = Instant.now();
      String accessToken = tokenProvider.generateAccessToken(userDetails, now);
      String refreshToken = tokenProvider.generateRefreshToken(userDetails, now);

      // 4. 토큰 정보 생성
      Instant accessExp = tokenProvider.getExpiryFromToken(accessToken);
      Instant refreshExp = tokenProvider.getExpiryFromToken(refreshToken);

      JwtInformation info = new JwtInformation(
          userDto, accessToken, accessExp, now,
          refreshToken, refreshExp, now
      );

      // 5. DB 저장
      registerJwtInformation(info);

      return new JwtTokens(accessToken, refreshToken);
    } catch (Exception e) {
      log.error("[JWT 서비스] : 사용자 ID {}의 JWT 토큰 발급 실패", user.getId(), e);
      // 트랜잭션 롤백 -> 기존 토큰 유효 상태 유지
      throw AuthenticationOAuthJwtException.withException("JWT 토큰 생성 실패", e.getMessage());
    }
  }
}
