package com.codeit.playlist.domain.auth.service.basic;

import com.codeit.playlist.domain.auth.exception.InvalidOrExpiredException;
import com.codeit.playlist.domain.auth.exception.JwtInternalServerErrorException;
import com.codeit.playlist.domain.auth.exception.RefreshTokenException;
import com.codeit.playlist.domain.auth.service.AuthService;
import com.codeit.playlist.domain.security.PlaylistUserDetails;
import com.codeit.playlist.domain.security.jwt.JwtInformation;
import com.codeit.playlist.domain.security.jwt.JwtRegistry;
import com.codeit.playlist.domain.security.jwt.JwtTokenProvider;
import com.codeit.playlist.domain.user.dto.data.UserDto;
import com.codeit.playlist.domain.user.dto.request.UserRoleUpdateRequest;
import com.codeit.playlist.domain.user.entity.Role;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.exception.UserNotFoundException;
import com.codeit.playlist.domain.user.mapper.UserMapper;
import com.codeit.playlist.domain.user.repository.UserRepository;
import com.nimbusds.jose.JOSEException;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class BasicAuthService implements AuthService {

  private final AuthenticationManager authenticationManager;
  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final JwtTokenProvider jwtTokenProvider;
  private final JwtRegistry jwtRegistry;
  private final UserDetailsService userDetailsService;

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public UserDto updateRole(UserRoleUpdateRequest request, UUID userId) { // 권한 업데이트 로직, ADMIN 만 가능
    return updateRoleInternal(request, userId);
  }

  @Override
  public UserDto updateRoleInternal(UserRoleUpdateRequest request, UUID userId) {
    log.debug("[사용자 관리] 사용자 권한 변경 시작 : userId={}", userId);

    User user = userRepository.findById(userId)
        .orElseThrow(() -> UserNotFoundException.withId(userId));

    Role oldRole = user.getRole();
    Role newRole = request.newRole();

    if (!oldRole.equals(newRole)) {
      user.updateRole(newRole);
      log.debug("[사용자 관리] 사용자 권한 변경 : userId={}, {} -> {}", userId, oldRole, newRole);

      // 역할 변경 시 해당 사용자의 모든 JWT 토큰 무효화
      jwtRegistry.invalidateJwtInformationByUserId(userId);
    }

    log.info("[사용자 관리] 사용자 권한 변경 완료 : userId={} , {} -> {}", userId, oldRole, newRole);

    return userMapper.toDto(user);
  }

  @Override
  public JwtInformation signIn(String username, String password) throws JOSEException {
    log.debug("[인증 관리] : 로그인 시작");

    Authentication auth = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(username, password)
    );

    SecurityContextHolder.getContext().setAuthentication(auth);

    PlaylistUserDetails userDetails = (PlaylistUserDetails) auth.getPrincipal();
    UserDto userDto = userDetails.getUserDto();

    jwtRegistry.invalidateJwtInformationByUserId(userDto.id());

    // Access 발급 시간
    Instant accessIssuedAt = Instant.now();
    String access = jwtTokenProvider.generateAccessToken(userDetails, accessIssuedAt);

    // Refresh 발급 시간
    Instant refreshIssuedAt = Instant.now();
    String refresh = jwtTokenProvider.generateRefreshToken(userDetails, refreshIssuedAt);

    // 만료 시간 추출
    Instant accessExp = jwtTokenProvider.getExpiryFromToken(access);
    Instant refreshExp = jwtTokenProvider.getExpiryFromToken(refresh);

    JwtInformation info = new JwtInformation(
        userDto,
        access, accessExp,
        accessIssuedAt,
        refresh, refreshExp,
        refreshIssuedAt
    );

    // DB 저장
    jwtRegistry.registerJwtInformation(info);
    log.info("[인증 관리] : 로그인 완료");

    return info;
  }

  @Override
  public JwtInformation refreshToken(String refreshToken) {
    log.debug("[인증 관리] : Token 발급 시작");

    if (refreshToken == null || refreshToken.isBlank()) {
      throw RefreshTokenException.withToken(refreshToken);
    }

      if (!jwtTokenProvider.validateRefreshToken(refreshToken)
          || !jwtRegistry.hasActiveJwtInformationByRefreshToken(refreshToken)) {
        throw InvalidOrExpiredException.withToken(refreshToken);
      }

      String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
      UserDetails userDetails = userDetailsService.loadUserByUsername(username);

      PlaylistUserDetails playlistUser = (PlaylistUserDetails) userDetails;

      try {

        Instant accessIssuedAt = Instant.now();
        Instant refreshIssuedAt = Instant.now();

        String newAccess = jwtTokenProvider.generateAccessToken(playlistUser, accessIssuedAt);
        String newRefresh = jwtTokenProvider.generateRefreshToken(playlistUser, refreshIssuedAt);

        Instant accessExp = jwtTokenProvider.getExpiryFromToken(newAccess);
        Instant refreshExp = jwtTokenProvider.getExpiryFromToken(newRefresh);

        JwtInformation info = new JwtInformation(
            playlistUser.getUserDto(),
            newAccess, accessExp, accessIssuedAt,
            newRefresh, refreshExp, refreshIssuedAt
        );

        boolean rotated = jwtRegistry.rotateJwtInformation(refreshToken, info);
        if (!rotated) {
          throw InvalidOrExpiredException.withToken(refreshToken);
        }
        log.info("[인증 관리] : Token 발급 완료 ");
        return info;

      } catch (JOSEException e) {
        throw JwtInternalServerErrorException.jwtError(e);
      }
    }

    @Override
    public void logout (String refreshToken){
      log.debug("[인증 관리] : 로그아웃 시작");
      if (refreshToken == null || refreshToken.isBlank()) {
        return;
      }

      if (jwtTokenProvider.validateRefreshToken(refreshToken)) {
        UUID userId = jwtTokenProvider.getUserId(refreshToken);
        jwtRegistry.invalidateJwtInformationByUserId(userId);
      }
      jwtRegistry.revokeByToken(refreshToken);
      log.info("[인증 관리] : 로그아웃 완료");
    }
  }
