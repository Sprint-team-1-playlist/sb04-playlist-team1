/*
package com.codeit.playlist.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.playlist.domain.auth.exception.AuthAccessDeniedException;
import com.codeit.playlist.domain.security.PlaylistUserDetails;
import com.codeit.playlist.domain.security.jwt.JwtRegistry;
import com.codeit.playlist.domain.user.dto.data.UserDto;
import com.codeit.playlist.domain.user.dto.request.ChangePasswordRequest;
import com.codeit.playlist.domain.user.dto.request.UserCreateRequest;
import com.codeit.playlist.domain.user.entity.Role;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.exception.EmailAlreadyExistsException;
import com.codeit.playlist.domain.user.exception.NewPasswordRequired;
import com.codeit.playlist.domain.user.exception.PasswordMustCharacters;
import com.codeit.playlist.domain.user.exception.UserNotFoundException;
import com.codeit.playlist.domain.user.mapper.UserMapper;
import com.codeit.playlist.domain.user.repository.UserRepository;
import com.codeit.playlist.domain.user.service.basic.BasicUserService;
import com.codeit.playlist.global.redis.TemporaryPasswordStore;
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
import org.springframework.test.util.ReflectionTestUtils;

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

  @Mock
  TemporaryPasswordStore temporaryPasswordStore;

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
  @DisplayName("회원가입 성공")
  void registerUserSuccess() {
    // Given
    UserCreateRequest request = new UserCreateRequest(
        "테스트",
        "test@example.com",
        "123456789"
    );

    User newUser = new User(
        "테스트",
        "123456789",
        "테스트",
        null,  // profileImageUrl
        Role.USER
    );

    User savedUser = new User(
        "테스트",
        "encodedPassword",
        "테스트",
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
  @DisplayName("회원가입 성공 - adminEmail일 경우 자동 ADMIN 권한 부여")
  void registerUserSuccessAssignAdminRole(){
    //Given
    String adminEmail = "admin@test.com";
    ReflectionTestUtils.setField(userService, "adminEmail", adminEmail);

    UserCreateRequest request = new UserCreateRequest("관리자", adminEmail, "password123");

    User newUser = new User(adminEmail, "password123", "관리자", null, null);

    when(userMapper.toEntity(request)).thenReturn(newUser);
    when(userRepository.existsByEmail(adminEmail)).thenReturn(false);
    when(passwordEncoder.encode("password123")).thenReturn("encodedPwd");
    when(userRepository.save(any(User.class))).thenReturn(newUser);
    when(userMapper.toDto(newUser)).thenReturn(dto);

    // When
    userService.registerUser(request);

    // Then
    assertEquals(Role.ADMIN, newUser.getRole());
  }

  @Test
  @DisplayName("회원가입 실패 - 이메일 중복")
  void registerUserFailEmailExists() {
    //Given
    UserCreateRequest request = new UserCreateRequest("name", "test@example.com", "password123");

    User newUser = new User("test@example.com", "password123", "name", null, Role.USER);

    //When
    when(userMapper.toEntity(request)).thenReturn(newUser);
    when(userRepository.existsByEmail(newUser.getEmail())).thenReturn(true);

    //Then
    assertThrows(EmailAlreadyExistsException.class, () -> userService.registerUser(request));
  }

  @Test
  @DisplayName("사용자 조회 성공")
  void findUserSuccess() {
      when(userRepository.findById(FIXED_ID)).thenReturn(Optional.of(user));
      when(userMapper.toDto(user)).thenReturn(dto);

      UserDto result = userService.find(FIXED_ID);

      assertEquals(dto.id(), result.id());
      assertEquals(dto.email(), result.email());
  }

  @Test
  @DisplayName("사용자 조회 실패 - 존재하지 않는 사용자")
  void findUserFailNotFound() {
      when(userRepository.findById(FIXED_ID)).thenReturn(Optional.empty());

      assertThrows(UserNotFoundException.class, () -> userService.find(FIXED_ID));
  }

  @Test
  @DisplayName("비밀번호 변경 성공")
  void changePasswordSuccess() {
    // Given
    ChangePasswordRequest request = new ChangePasswordRequest("newPassword123");

    UserDto dto = new UserDto(
        FIXED_ID,
        LocalDateTime.now(),
        user.getEmail(),
        user.getName(),
        user.getProfileImageUrl(),
        user.getRole(),
        user.isLocked()
    );

    PlaylistUserDetails principal = new PlaylistUserDetails(dto, user.getPassword());

    when(authentication.getPrincipal()).thenReturn(principal);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);

    when(passwordEncoder.encode("newPassword123")).thenReturn("encodedPwd");

    // When
    userService.changePassword(FIXED_ID, request);

    // Then
    verify(passwordEncoder).encode("newPassword123");
    verify(userRepository).changePassword(eq(FIXED_ID), eq("encodedPwd"));
    verify(jwtRegistry).invalidateJwtInformationByUserId(FIXED_ID);
    verify(temporaryPasswordStore).delete(FIXED_ID);
  }


  @Test
  @DisplayName("비밀번호 변경 실패 - 인증 정보 없음")
  void changePasswordFailNoAuthentication() {
    SecurityContextHolder.clearContext(); // 인증 정보 없음

    ChangePasswordRequest request = new ChangePasswordRequest("validPass123!");

    assertThrows(AuthAccessDeniedException.class, () ->
        userService.changePassword(FIXED_ID, request)
    );
  }

  @Test
  @DisplayName("비밀번호 변경 실패 - Principal 타입이 PlaylistUserDetails가 아님")
  void changePasswordFailInvalidPrincipalType() {
    when(authentication.getPrincipal()).thenReturn("anonymous");
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);

    assertThrows(AuthAccessDeniedException.class, () ->
        userService.changePassword(FIXED_ID, new ChangePasswordRequest("validPass123"))
    );
  }

  @Test
  @DisplayName("비밀번호 변경 실패 - 로그인한 사용자와 대상 userId 불일치")
  void changePasswordFailNotOwner() {
    UUID otherUserId = UUID.randomUUID(); // 다른 사람

    PlaylistUserDetails principal =
        new PlaylistUserDetails(dto, user.getPassword());

    when(authentication.getPrincipal()).thenReturn(principal);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);

    ChangePasswordRequest request = new ChangePasswordRequest("validPass123");

    assertThrows(AuthAccessDeniedException.class, () ->
        userService.changePassword(otherUserId, request)
    );
  }
  @Test
  @DisplayName("비밀번호 변경 실패 - 비밀번호 null 또는 공백")
  void changePasswordFail_BlankPassword() {
    PlaylistUserDetails principal = new PlaylistUserDetails(dto, user.getPassword());
    when(authentication.getPrincipal()).thenReturn(principal);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);

    assertThrows(NewPasswordRequired.class, () ->
        userService.changePassword(FIXED_ID, new ChangePasswordRequest(" "))
    );
  }

  @Test
  @DisplayName("비밀번호 변경 실패 - 비밀번호 길이 부족(8자 미만)")
  void changePasswordFail_ShortPassword() {
    PlaylistUserDetails principal = new PlaylistUserDetails(dto, user.getPassword());
    when(authentication.getPrincipal()).thenReturn(principal);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);

    assertThrows(PasswordMustCharacters.class, () ->
        userService.changePassword(FIXED_ID, new ChangePasswordRequest("1234567"))
    );
  }

}
*/
