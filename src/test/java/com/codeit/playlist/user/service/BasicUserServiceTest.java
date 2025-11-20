package com.codeit.playlist.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.playlist.domain.security.PlaylistUserDetails;
import com.codeit.playlist.domain.security.jwt.JwtRegistry;
import com.codeit.playlist.domain.user.dto.data.UserDto;
import com.codeit.playlist.domain.user.dto.request.ChangePasswordRequest;
import com.codeit.playlist.domain.user.dto.request.UserCreateRequest;
import com.codeit.playlist.domain.user.entity.Role;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.mapper.UserMapper;
import com.codeit.playlist.domain.user.repository.UserRepository;
import com.codeit.playlist.domain.user.service.basic.BasicUserService;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class BasicUserServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private UserMapper userMapper;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private JwtRegistry jwtRegistry;

  @InjectMocks
  private BasicUserService userService;

  @Mock
  private Authentication authentication;

  @Mock
  private SecurityContext securityContext;

  private UUID FIXED_ID;
  private User user;
  private UserDto dto;

  @BeforeEach
  void setUp() {
    FIXED_ID = UUID.randomUUID();

    user = new User(
        "test@example.com",
        "oldPassword",
        "테스트유저",
        null,
        Role.USER
    );

    // SecurityContext 설정
    dto = new UserDto(
        FIXED_ID,
        null,
        user.getEmail(),
        user.getName(),
        user.getProfileImageUrl(),
        user.getRole(),
        false
    );
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("사용자 생성 테스트 성공")
  void registerUserSuccess() {
    // Given
    UserCreateRequest request = new UserCreateRequest(
        "강은혁",
        "dmsgur7305@naver.com",
        "123456789"
    );

    User newUser = new User(
        "dmsgur7305@naver.com",
        "123456789",
        "강은혁",
        null,  // profileImageUrl
        Role.USER
    );

    User savedUser = new User(
        "dmsgur7305@naver.com",
        "encodedPassword",
        "강은혁",
        null,
        Role.USER
    );

    UserDto userDto = new UserDto(
        UUID.randomUUID(),
        LocalDateTime.now(),
        savedUser.getEmail(),
        savedUser.getName(),
        savedUser.getProfileImageUrl(),
        savedUser.getRole(),
        savedUser.isLocked()
    );

    // Mock 동작 정의
    when(userMapper.toEntity(request)).thenReturn(newUser);
    when(userRepository.existsByEmail(newUser.getEmail())).thenReturn(false);
    when(passwordEncoder.encode("123456789")).thenReturn("encodedPassword");
    when(userRepository.save(any(User.class))).thenReturn(savedUser);
    when(userMapper.toDto(any(User.class))).thenReturn(userDto);

    // When
    UserDto result = userService.registerUser(request);

    // Then
    assertNotNull(result);
    assertEquals(userDto.email(), result.email());
    assertEquals(userDto.name(), result.name());
    assertEquals(userDto.role(), result.role());
    assertEquals(userDto.locked(), result.locked());

    verify(userRepository).existsByEmail(newUser.getEmail());
    verify(passwordEncoder).encode("123456789");
    verify(userRepository).save(any(User.class));
  }

  @Test
  @DisplayName("비밀번호 변경 성공")
  void changePasswordSuccess() {
    // Given
    ChangePasswordRequest request = new ChangePasswordRequest("newPassword123");
    PlaylistUserDetails principal = new PlaylistUserDetails(dto, user.getPassword());

    when(authentication.getPrincipal()).thenReturn(principal);
    when(securityContext.getAuthentication()).thenReturn(authentication);

    SecurityContextHolder.setContext(securityContext);

    when(userRepository.findById(FIXED_ID)).thenReturn(Optional.of(user));
    when(passwordEncoder.encode("newPassword123")).thenReturn("encodedPwd");

    // When
    userService.changePassword(FIXED_ID, request);

    // Then
    verify(passwordEncoder).encode("newPassword123");
    verify(userRepository).changePassword(eq(FIXED_ID), eq("encodedPwd"));
    verify(jwtRegistry).invalidateJwtInformationByUserId(FIXED_ID);
  }

}
