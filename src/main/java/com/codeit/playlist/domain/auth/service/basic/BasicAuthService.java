package com.codeit.playlist.domain.auth.service.basic;

import com.codeit.playlist.domain.auth.exception.InvalidOrExpiredException;
import com.codeit.playlist.domain.auth.exception.JwtInternalServerErrorException;
import com.codeit.playlist.domain.auth.exception.RefreshTokenException;
import com.codeit.playlist.domain.auth.service.AuthService;
import com.codeit.playlist.domain.notification.dto.data.NotificationDto;
import com.codeit.playlist.domain.notification.entity.Level;
import com.codeit.playlist.domain.security.PlaylistUserDetails;
import com.codeit.playlist.domain.security.jwt.JwtInformation;
import com.codeit.playlist.domain.security.jwt.JwtRegistry;
import com.codeit.playlist.domain.security.jwt.JwtTokenProvider;
import com.codeit.playlist.domain.sse.repository.SseEmitterRepository;
import com.codeit.playlist.domain.user.dto.data.UserDto;
import com.codeit.playlist.domain.user.dto.request.UserRoleUpdateRequest;
import com.codeit.playlist.domain.user.entity.Role;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.exception.UserNotFoundException;
import com.codeit.playlist.domain.user.mapper.UserMapper;
import com.codeit.playlist.domain.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
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
  private final StringRedisTemplate redisTemplate;
  private final PasswordEncoder passwordEncoder;
  private final SseEmitterRepository sseEmitterRepository;

  private final ObjectMapper objectMapper;
  private final KafkaTemplate<String, String> kafkaTemplate;

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
    Role newRole = request.role();

    if (!oldRole.equals(newRole)) {
      user.updateRole(newRole);
      log.debug("[사용자 관리] 사용자 권한 변경 : userId={}, {} -> {}", userId, oldRole, newRole);

      // 역할 변경 시 해당 사용자의 모든 JWT 토큰 무효화
      jwtRegistry.invalidateJwtInformationByUserId(userId);

      sendRoleChangedNotification(userId, oldRole, newRole);

      log.info("[사용자 관리] 사용자 권한 변경 완료 : userId={} , {} -> {}", userId, oldRole, newRole);

    } else {
      log.info("[사용자 관리] 권한 변경 없음 (동일 권한 요청 무시): userId={}, role={}", userId, newRole);
    }

    return userMapper.toDto(user);
  }

  @Override
  public JwtInformation signIn(String username, String password) throws JOSEException {
    log.debug("[인증 관리] : 로그인 시작");

    // AuthenticationManager를 통해 인증 (계정 상태 검증 포함)
    Authentication auth = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(username, password)
    );

    SecurityContextHolder.getContext().setAuthentication(auth);

    PlaylistUserDetails userDetails = (PlaylistUserDetails) auth.getPrincipal();
    UUID userId = userDetails.getUserDto().id();

    // 임시 비밀번호 사용 여부 확인 및 처리
    String redisKey = "temp-password:" + userId;
    String storedHashedTempPassword = redisTemplate.opsForValue().get(redisKey);

    if (storedHashedTempPassword != null && passwordEncoder.matches(password,
        storedHashedTempPassword)) {
      log.info("[보안 감사] 임시 비밀번호 사용: userId={}", userId);
      // 임시 비밀번호는 1회용이므로 삭제
      redisTemplate.delete(redisKey);
    }
    // 기존 JWT 갱신 처리
    jwtRegistry.invalidateJwtInformationByUserId(userId);

    JwtInformation info = generateJwt(userDetails);
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
      throw new InvalidOrExpiredException();
    }

    String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
    UserDetails userDetails;
    try {
      userDetails = userDetailsService.loadUserByUsername(username);
    } catch (UsernameNotFoundException e) {
      throw new InvalidOrExpiredException();
    }
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
        throw new InvalidOrExpiredException();
      }
      log.info("[인증 관리] : Token 발급 완료 ");
      return info;

    } catch (JOSEException e) {
      throw JwtInternalServerErrorException.jwtError(e);
    }
  }

  @Override
  public void logout(String refreshToken) {
    log.debug("[인증 관리] : 로그아웃 시작");
    if (refreshToken == null || refreshToken.isBlank()) {
      return;
    }

    if (jwtTokenProvider.validateRefreshToken(refreshToken)) {
      UUID userId = jwtTokenProvider.getUserId(refreshToken);
      jwtRegistry.invalidateJwtInformationByUserId(userId);

      sseEmitterRepository.closeByUserId(userId);
    }
    jwtRegistry.revokeByToken(refreshToken);
    log.info("[인증 관리] : 로그아웃 완료");
  }

  private JwtInformation generateJwt(PlaylistUserDetails userDetails) throws JOSEException {

    UserDto userDto = userDetails.getUserDto();

    Instant accessIssuedAt = Instant.now();
    String access = jwtTokenProvider.generateAccessToken(userDetails, accessIssuedAt);

    Instant refreshIssuedAt = Instant.now();
    String refresh = jwtTokenProvider.generateRefreshToken(userDetails, refreshIssuedAt);

    Instant accessExp = jwtTokenProvider.getExpiryFromToken(access);
    Instant refreshExp = jwtTokenProvider.getExpiryFromToken(refresh);

    return new JwtInformation(
        userDto,
        access, accessExp, accessIssuedAt,
        refresh, refreshExp, refreshIssuedAt
    );
  }

  private void sendRoleChangedNotification(UUID userId, Role oldRole, Role newRole) {
    //권한 변경 알림 생성
    String title = "내 권한이 변경되었어요.";
    String contentMsg = String.format("내 권한이 [%s]에서 [%s](으)로 변경되었습니다.",oldRole, newRole);

    NotificationDto notificationDto = new NotificationDto(null, null, userId,
            title, contentMsg, Level.INFO);

    try {
      String payload = objectMapper.writeValueAsString(notificationDto);
      kafkaTemplate.send("playlist.NotificationDto", payload);

      log.info("[사용자 관리] 권한 변경 알림 이벤트 발행 완료 : userId={}", userId);
    } catch (JsonProcessingException e) {
      //권한 변경 자체는 막지 않도록 로그만 남김
      log.error("[사용자 관리] 권한 변경 알림 이벤트 직렬화 실패 : userId={}", userId, e);
    }
  }

}
