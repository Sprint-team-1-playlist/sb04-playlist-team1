package com.codeit.playlist.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.playlist.domain.auth.exception.InvalidOrExpiredException;
import com.codeit.playlist.domain.auth.service.basic.BasicAuthService;
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
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class BasicAuthServiceTest {


  @Mock
  private AuthenticationManager authenticationManager;

  @Mock
  private UserRepository userRepository;

  @Mock
  private UserMapper userMapper;

  @Mock
  private JwtTokenProvider jwtTokenProvider;

  @Mock
  private JwtRegistry jwtRegistry;

  @Mock
  private UserDetailsService userDetailsService;

  @Mock
  private StringRedisTemplate redisTemplate;

  @Mock
  private PasswordEncoder passwordEncoder;

  @InjectMocks
  private BasicAuthService authService;

  @Mock
  Authentication authentication;

  @Mock
  ValueOperations<String, String> valueOps;

  @Mock
  private SecurityContext securityContext;

  private UUID FIXED_ID;
  private PlaylistUserDetails userDetails;
  private UserDto dto;


  @BeforeEach
  void setUp() {
    FIXED_ID = UUID.randomUUID();

    dto = new UserDto(
        FIXED_ID,
        null,
        "test@email.com",
        "testUser",
        null,
        Role.USER,
        false
    );

    userDetails = new PlaylistUserDetails(dto, "encodedPassword");

  }

  @Test
  @DisplayName("권한 수정 성공")
  void updateRoleInternalSuccess() {
    // Given
    UUID userId = UUID.randomUUID();
    User user = new User("test@test.com", "pwd", "유저", null, Role.USER);
    UserRoleUpdateRequest request = new UserRoleUpdateRequest(Role.ADMIN);

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(userMapper.toDto(user)).thenReturn(
        new UserDto(userId, null, user.getEmail(), user.getName(), null, Role.ADMIN, false)
    );

    // When
    UserDto result = authService.updateRoleInternal(request, userId);

    // Then
    assertEquals(Role.ADMIN, result.role());
    verify(jwtRegistry).invalidateJwtInformationByUserId(userId);
  }

  @Test
  @DisplayName("권한 수정 실패 - 역할 동일, 변경 없어야함")
  void updateRoleInternalNoChange() {
    //given
    User user = new User("test@test.com", "pwd", "유저", null, Role.USER);
    UserRoleUpdateRequest request = new UserRoleUpdateRequest(Role.USER);

    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
    when(userMapper.toDto(user)).thenReturn(
        new UserDto(FIXED_ID, null, user.getEmail(), user.getName(), null, Role.USER, false));

    //when
    authService.updateRoleInternal(request, user.getId());

    //then
    verify(jwtRegistry, org.mockito.Mockito.never())
        .invalidateJwtInformationByUserId(any());
  }

  @Test
  @DisplayName("권한 수정 실패 - 유저 존재하지 않음")
  void updateRoleInternalUserNotFound() {
    UserRoleUpdateRequest request = new UserRoleUpdateRequest(Role.ADMIN);

    when(userRepository.findById(FIXED_ID)).thenReturn(Optional.empty());

    assertThrows(UserNotFoundException.class,
        () -> authService.updateRoleInternal(request, FIXED_ID));
  }

  @Test
  @DisplayName("로그인 성공 - 일반 비밀번호")
  void signInSuccess() throws Exception {

    when(authenticationManager.authenticate(any())).thenReturn(authentication);
    when(authentication.getPrincipal()).thenReturn(userDetails);

    when(redisTemplate.opsForValue()).thenReturn(valueOps);
    when(valueOps.get("temp-password:" + FIXED_ID)).thenReturn(null); // 임시 비밀번호 없음

    // JWT 생성 mock
    when(jwtTokenProvider.generateAccessToken(any(), any())).thenReturn("ACCESS");
    when(jwtTokenProvider.generateRefreshToken(any(), any())).thenReturn("REFRESH");
    when(jwtTokenProvider.getExpiryFromToken("ACCESS")).thenReturn(Instant.now().plusSeconds(3600));
    when(jwtTokenProvider.getExpiryFromToken("REFRESH")).thenReturn(Instant.now().plusSeconds(7200));

    JwtInformation result = authService.signIn("test", "password");

    assertNotNull(result);
    assertEquals("ACCESS", result.accessToken());
    assertEquals("REFRESH", result.refreshToken());

    verify(jwtRegistry).invalidateJwtInformationByUserId(FIXED_ID);
    verify(jwtRegistry).registerJwtInformation(any());
  }

  @Test
  @DisplayName("로그인 성공 - 임시 비밀번호 사용 시 redis 삭제됨")
  void signInSuccessWithTempPassword() throws Exception {

    when(authenticationManager.authenticate(any())).thenReturn(authentication);
    when(authentication.getPrincipal()).thenReturn(userDetails);

    when(redisTemplate.opsForValue()).thenReturn(valueOps);
    when(valueOps.get("temp-password:" + FIXED_ID)).thenReturn("hashedTemp");

    when(passwordEncoder.matches("temp1234", "hashedTemp")).thenReturn(true);

    when(jwtTokenProvider.generateAccessToken(any(), any())).thenReturn("ACCESS");
    when(jwtTokenProvider.generateRefreshToken(any(), any())).thenReturn("REFRESH");
    when(jwtTokenProvider.getExpiryFromToken("ACCESS")).thenReturn(Instant.now().plusSeconds(3600));
    when(jwtTokenProvider.getExpiryFromToken("REFRESH")).thenReturn(Instant.now().plusSeconds(7200));

    JwtInformation result = authService.signIn("test", "temp1234");

    verify(redisTemplate).delete("temp-password:" + FIXED_ID); // ⭐ 1회용 임시 비밀번호 삭제
    verify(jwtRegistry).invalidateJwtInformationByUserId(FIXED_ID);
    verify(jwtRegistry).registerJwtInformation(any());

    assertNotNull(result);
  }
  
  @Test
  @DisplayName("로그인 실패 : BadCredentialsException")
  void signInBadCredentials() {
    when(authenticationManager.authenticate(any()))
        .thenThrow(new org.springframework.security.authentication.BadCredentialsException("bad"));

    assertThrows(
        org.springframework.security.authentication.BadCredentialsException.class,
        () -> authService.signIn("test", "wrong")
    );
  }

  @Test
  @DisplayName("로그인 실패 - UsernameNotFoundException")
  void signInUsernameNotFound() {

    when(authenticationManager.authenticate(any()))
        .thenThrow(new org.springframework.security.core.userdetails.UsernameNotFoundException("not found"));

    assertThrows(
        org.springframework.security.core.userdetails.UsernameNotFoundException.class,
        () -> authService.signIn("test", "wrong")
    );
  }

  @Test
  @DisplayName("refreshToken: rotate 실패 → InvalidOrExpiredException")
  void refreshTokenRotateFail() {
    String token = "refresh123";

    when(jwtTokenProvider.validateRefreshToken(token)).thenReturn(true);
    when(jwtRegistry.hasActiveJwtInformationByRefreshToken(token)).thenReturn(true);
    when(jwtTokenProvider.getUsernameFromToken(token)).thenReturn("user@test.com");

    UserDto dto = new UserDto(UUID.randomUUID(), null, "user@test.com", "user", null, Role.USER, false);
    PlaylistUserDetails userDetails = new PlaylistUserDetails(dto, "pwd");

    when(userDetailsService.loadUserByUsername("user@test.com"))
        .thenReturn(userDetails);

    when(jwtRegistry.rotateJwtInformation(eq(token), any())).thenReturn(false);

    assertThrows(InvalidOrExpiredException.class,
        () -> authService.refreshToken(token));
  }

  @Test
  @DisplayName("logout: refreshToken 유효하면 invalidate + revoke 호출")
  void logoutSuccess() {
    String token = "ref";
    UUID userId = UUID.randomUUID();

    when(jwtTokenProvider.validateRefreshToken(token)).thenReturn(true);
    when(jwtTokenProvider.getUserId(token)).thenReturn(userId);

    authService.logout(token);

    verify(jwtRegistry).invalidateJwtInformationByUserId(userId);
    verify(jwtRegistry).revokeByToken(token);
  }
}
