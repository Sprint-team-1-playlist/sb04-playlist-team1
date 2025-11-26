package com.codeit.playlist.user.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.playlist.domain.auth.exception.AuthAccessDeniedException;
import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.security.PlaylistUserDetails;
import com.codeit.playlist.domain.security.jwt.JwtRegistry;
import com.codeit.playlist.domain.user.dto.data.UserDto;
import com.codeit.playlist.domain.user.dto.request.ChangePasswordRequest;
import com.codeit.playlist.domain.user.dto.request.UserCreateRequest;
import com.codeit.playlist.domain.user.dto.request.UserLockUpdateRequest;
import com.codeit.playlist.domain.user.dto.response.CursorResponseUserDto;
import com.codeit.playlist.domain.user.entity.Role;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.exception.EmailAlreadyExistsException;
import com.codeit.playlist.domain.user.exception.NewPasswordRequired;
import com.codeit.playlist.domain.user.exception.PasswordMustCharacters;
import com.codeit.playlist.domain.user.exception.UserNotFoundException;
import com.codeit.playlist.domain.user.mapper.UserMapper;
import com.codeit.playlist.domain.user.repository.UserRepository;
import com.codeit.playlist.domain.user.repository.UserRepositoryCustom;
import com.codeit.playlist.domain.user.service.basic.BasicUserService;
import com.codeit.playlist.global.redis.TemporaryPasswordStore;
import java.time.LocalDateTime;
import java.util.List;
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
  private UserRepositoryCustom userRepositoryCustom;

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

  @Test
  @DisplayName("사용자 목록 조회 - 커서 페이징 성공")
  void findUserListSuccess() {
    // Given
    String emailLike = null;
    String roleEqual = null;
    Boolean isLocked = null;
    String cursor = null;
    UUID idAfter = null;
    int limit = 1;
    String sortBy = "email";
    SortDirection direction = SortDirection.ASCENDING;

    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();

    User user1 = new User("a@test.com", "pw", "유저1", null, Role.USER);
    User user2 = new User("b@test.com", "pw", "유저2", null, Role.USER);

    setId(user1, id1);
    setId(user2, id2);

    List<User> repoResult = List.of(user1, user2);

    when(userRepositoryCustom.countUsers(emailLike, roleEqual, isLocked))
        .thenReturn(2L);

    when(userRepositoryCustom.searchUsers(
        emailLike,
        roleEqual,
        isLocked,
        cursor,
        idAfter,
        limit,
        sortBy,
        direction
    )).thenReturn(repoResult);

    when(userMapper.toDto(any(User.class)))
        .thenAnswer(invocation -> {
          User u = invocation.getArgument(0);
          return new UserDto(
              u.getId(),
              null,
              u.getEmail(),
              u.getName(),
              u.getProfileImageUrl(),
              u.getRole(),
              u.isLocked()
          );
        });

    // When
    CursorResponseUserDto response = userService.findUserList(
        emailLike,
        roleEqual,
        isLocked,
        cursor,
        idAfter,
        limit,
        sortBy,
        direction
    );

    // Then
    assertThat(response.data()).hasSize(1);
    assertThat(response.data().get(0).email()).isEqualTo("a@test.com");

    assertThat(response.hasNext()).isTrue();

    assertThat(response.nextCursor()).isEqualTo("a@test.com");
    assertThat(response.nextIdAfter()).isEqualTo(id1);

    assertThat(response.totalCount()).isEqualTo(2L);

    verify(userRepositoryCustom).countUsers(emailLike, roleEqual, isLocked);
    verify(userRepositoryCustom).searchUsers(
        emailLike,
        roleEqual,
        isLocked,
        cursor,
        idAfter,
        limit,
        sortBy,
        direction
    );
  }

  @Test
  @DisplayName("사용자 잠금상태 변경 성공  repository update + jwt invalidate 호출됨")
  void updateUserLockedSuccess() {
    // given
    when(userRepository.findById(FIXED_ID)).thenReturn(Optional.of(user));
    UserLockUpdateRequest request = new UserLockUpdateRequest(true);

    // when
    userService.updateUserLocked(FIXED_ID, request);

    // then
    verify(userRepository, times(1)).updateUserLocked(FIXED_ID, true);
    verify(jwtRegistry, times(1)).invalidateJwtInformationByUserId(FIXED_ID);
  }

  @Test
  @DisplayName("사용자 잠금상태 변경 - 변경 상태가 기존과 동일하면 IllegalArgumentException 발생")
  void updateUserLockedSameStateThrowsException() {
    // given
    user.setLocked(true);
    // 현재 locked=true
    when(userRepository.findById(FIXED_ID)).thenReturn(Optional.of(user));
    UserLockUpdateRequest request = new UserLockUpdateRequest(true);

    // expect
    assertThatThrownBy(() -> userService.updateUserLocked(FIXED_ID, request))
        .isInstanceOf(IllegalArgumentException.class);

    verify(userRepository, never()).updateUserLocked(any(), anyBoolean());
    verify(jwtRegistry, never()).invalidateJwtInformationByUserId(any());
  }

  @Test
  @DisplayName("사용자 잠금상태 변경 - 존재하지 않는 사용자 → UserNotFoundException 발생")
  void updateUserLockedNotFoundThrowsException() {
    // given
    when(userRepository.findById(FIXED_ID)).thenReturn(Optional.empty());
    UserLockUpdateRequest request = new UserLockUpdateRequest(true);

    // expect
    assertThatThrownBy(() -> userService.updateUserLocked(FIXED_ID, request))
        .isInstanceOf(UserNotFoundException.class);

    verify(userRepository, never()).updateUserLocked(any(), anyBoolean());
    verify(jwtRegistry, never()).invalidateJwtInformationByUserId(any());
  }

  // 유틸: 테스트에서 ID 값을 강제로 세팅하는 메서드
  private void setId(Object target, UUID id) {
    Class<?> clazz = target.getClass();

    while (clazz != null) {
      for (var field : clazz.getDeclaredFields()) {
        if (field.getType().equals(UUID.class)) {
          field.setAccessible(true);
          try {
            field.set(target, id);
            return;
          } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
          }
        }
      }
      clazz = clazz.getSuperclass();
    }

    throw new RuntimeException("UUID 타입의 PK 필드를 찾을 수 없습니다.");
  }
}
