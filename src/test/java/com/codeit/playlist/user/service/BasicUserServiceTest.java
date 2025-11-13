package com.codeit.playlist.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.playlist.domain.user.dto.data.UserDto;
import com.codeit.playlist.domain.user.dto.request.UserCreateRequest;
import com.codeit.playlist.domain.user.entity.Role;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.mapper.UserMapper;
import com.codeit.playlist.domain.user.repository.UserRepository;
import com.codeit.playlist.domain.user.service.basic.BasicUserService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class BasicUserServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private UserMapper userMapper;

  @Mock
  private PasswordEncoder passwordEncoder;

  @InjectMocks
  private BasicUserService BasicuserService;

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
        "uuid-string",
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
    BasicUserService userService = new BasicUserService(userRepository, passwordEncoder, userMapper);
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

}
