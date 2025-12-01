package com.codeit.playlist.user.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.playlist.domain.auth.service.EmailService;
import com.codeit.playlist.domain.user.dto.data.TempPasswordIssuedEvent;
import com.codeit.playlist.domain.user.dto.request.ResetPasswordRequest;
import com.codeit.playlist.domain.user.entity.Role;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.exception.UserNotFoundException;
import com.codeit.playlist.domain.user.repository.UserRepository;
import com.codeit.playlist.domain.user.service.basic.BasicPasswordResetService;
import com.codeit.playlist.global.redis.TemporaryPasswordStore;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

public class BasicPasswordResetServiceTest {

   @Mock
   private UserRepository userRepository;

   @Mock
   private EmailService emailService;

   @Mock
   private TemporaryPasswordStore tempStore;

   @Mock
   private PasswordEncoder passwordEncoder;

   @Mock
   private ApplicationEventPublisher publisher;

   @InjectMocks
   private BasicPasswordResetService service;

  private User user;
  private UUID FIXED_ID;
  private ResetPasswordRequest request;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    FIXED_ID = UUID.randomUUID();

    user = new User(
        "test@example.com",
        "oldPassword",
        "테스트유저",
        null,
        Role.USER
    );

    ReflectionTestUtils.setField(user, "id", FIXED_ID);

    when(userRepository.findByEmail("test@test.com"))
        .thenReturn(Optional.of(user));


    request = new ResetPasswordRequest("test@test.com");

    // 실제 암호는 랜덤인데 테스트는 고정해줘야 검증이 가능함
    when(passwordEncoder.encode(anyString())).thenReturn("ENCODED_VALUE");

    // activeProfile 값을 직접 세팅
    ReflectionTestUtils.setField(service, "activeProfile", "dev");
  }

  @Test
  @DisplayName("임시 비밀번호 발급 성공 - 저장 + 이메일발송 + 비밀번호 변경")
  void sendTemporaryPasswordSuccess() {
    //given
    when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));

    //when
    service.sendTemporaryPassword(request);

    //then
    verify(userRepository, times(1)).findByEmail(request.email());
    verify(tempStore, times(1))
        .save(eq(FIXED_ID), eq("ENCODED_VALUE"), eq(180L)); // TTL = 3분
    verify(userRepository, times(1))
        .changePassword(eq(FIXED_ID), eq("ENCODED_VALUE"));
    verify(publisher, times(1))
        .publishEvent(any(TempPasswordIssuedEvent.class));
  }

  @Test
  @DisplayName("임시 비밀번호 발급 실패 - 존재하지 않는 사용자 UserNotFoundException 발생")
  void sendTemporaryPasswordUserNotFound() {
    //given
    when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());

    //expect
    assertThrows(UserNotFoundException.class,
        () -> service.sendTemporaryPassword(request));

    verify(userRepository, times(1)).findByEmail(request.email());
    verify(tempStore, never()).save(any(), any(), anyLong());
    verify(emailService, never()).sendTemporaryPassword(anyString(), anyString());
  }

}
