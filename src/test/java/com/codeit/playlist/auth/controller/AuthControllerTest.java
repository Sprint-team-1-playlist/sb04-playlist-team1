package com.codeit.playlist.auth.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.playlist.domain.auth.controller.AuthController;
import com.codeit.playlist.domain.auth.passwordratelimit.RateLimitService;
import com.codeit.playlist.domain.auth.service.AuthService;
import com.codeit.playlist.domain.security.jwt.JwtInformation;
import com.codeit.playlist.domain.security.jwt.JwtTokenProvider;
import com.codeit.playlist.domain.user.dto.data.UserDto;
import com.codeit.playlist.domain.user.entity.Role;
import com.codeit.playlist.domain.user.service.PasswordResetService;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

  @Mock AuthService authService;
  @Mock JwtTokenProvider jwtTokenProvider;
  @Mock PasswordResetService passwordResetService;
  @Mock RateLimitService rateLimitService;

  private static final Instant NOW = Instant.now();
  private static final Instant ACCESS_EXPIRES = NOW.plusSeconds(60 * 15);   // 15분
  private static final Instant REFRESH_EXPIRES = NOW.plusSeconds(60 * 60 * 24 * 7); // 7일

  @InjectMocks
  AuthController authController;

  MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders
        .standaloneSetup(authController)
        .build();
  }

  private UserDto mockUser() {
    return new UserDto(
        UUID.randomUUID(),
        LocalDateTime.now(),
        "test@test.com",
        "테스트유저",
        null,
        Role.USER,
        false
    );
  }

  @Test
  @DisplayName("GET /api/auth/csrf-token - CSRF 토큰 쿠키 발급")
  void getCsrfToken() throws Exception {
    mockMvc.perform(get("/api/auth/csrf-token"))
        .andExpect(status().isNoContent())
        .andExpect(header().exists("Set-Cookie"));
  }

  @Test
  @DisplayName("POST /api/auth/sign-in - 로그인 성공")
  void signInSuccess() throws Exception {
    JwtInformation info = new JwtInformation(
        mockUser(),
        "access-token",
        ACCESS_EXPIRES,
        NOW,
        "refresh-token",
        REFRESH_EXPIRES,
        NOW
    );

    when(authService.signIn(anyString(), anyString())).thenReturn(info);
    when(jwtTokenProvider.generateRefreshTokenCookie(anyString()))
        .thenReturn(ResponseCookie.from("REFRESH_TOKEN", "refresh").build());

    mockMvc.perform(post("/api/auth/sign-in")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("username", "test@test.com")
            .param("password", "password123")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").value("access-token"));
  }

  @Test
  @DisplayName("POST /api/auth/refresh - 토큰 재발급")
  void refreshToken() throws Exception {
    JwtInformation info = new JwtInformation(
        mockUser(),
        "new-access",
        ACCESS_EXPIRES,
        NOW,
        "refresh-token",
        REFRESH_EXPIRES,
        NOW
    );

    when(authService.refreshToken(anyString())).thenReturn(info);
    when(jwtTokenProvider.generateRefreshTokenCookie(anyString()))
        .thenReturn(ResponseCookie.from("REFRESH_TOKEN", "new").build());

    mockMvc.perform(post("/api/auth/refresh")
            .cookie(new Cookie("REFRESH_TOKEN", "old-refresh")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").value("new-access"))
        .andExpect(header().exists("Set-Cookie"));
  }

  @Test
  @DisplayName("POST /api/auth/sign-out - 로그아웃 (쿠키 제거)")
  void signOut() throws Exception {
    mockMvc.perform(post("/api/auth/sign-out")
            .cookie(new Cookie("REFRESH_TOKEN", "refresh")))
        .andExpect(status().isNoContent())
        .andExpect(header().stringValues("Set-Cookie", org.hamcrest.Matchers.hasItem(
            org.hamcrest.Matchers.containsString("REFRESH_TOKEN=")
        )));
  }

  @Test
  @DisplayName("POST /api/auth/reset-password - 비밀번호 초기화 성공")
  void resetPasswordSuccess() throws Exception {
    when(rateLimitService.isAllowed(anyString())).thenReturn(true);

    mockMvc.perform(post("/api/auth/reset-password")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "email": "test@test.com"
                }
                """))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("POST /api/auth/reset-password - 요청 제한 초과 (429)")
  void resetPasswordRateLimitExceeded() throws Exception {
    when(rateLimitService.isAllowed(anyString())).thenReturn(false);

    mockMvc.perform(post("/api/auth/reset-password")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "email": "test@test.com"
                }
                """))
        .andExpect(status().isTooManyRequests());
  }
}