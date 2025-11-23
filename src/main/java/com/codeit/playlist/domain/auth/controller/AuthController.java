package com.codeit.playlist.domain.auth.controller;

import com.codeit.playlist.domain.auth.passwordratelimit.RateLimitService;
import com.codeit.playlist.domain.auth.service.AuthService;
import com.codeit.playlist.domain.security.jwt.JwtInformation;
import com.codeit.playlist.domain.security.jwt.JwtTokenProvider;
import com.codeit.playlist.domain.user.dto.data.JwtDto;
import com.codeit.playlist.domain.user.dto.request.ResetPasswordRequest;
import com.codeit.playlist.domain.user.dto.request.SignInRequest;
import com.codeit.playlist.domain.user.service.PasswordResetService;
import com.nimbusds.jose.JOSEException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;
  private final JwtTokenProvider jwtTokenProvider;
  private final PasswordResetService passwordResetService;
  private final RateLimitService rateLimitService;

  @GetMapping("/csrf-token")
  public ResponseEntity<Void> getCsrfToken(CsrfToken csrfToken) {
    log.debug("[인증 관리] : CSRF 토큰 요청 시작");

    if (csrfToken != null) {
      log.debug("[인증 관리] CSRF 토큰이 존재합니다.");
    } else {
      log.trace("[인증 관리] : CSRF 토큰이 존재하지 않습니다.");
    }

    log.info(("[인증 관리] : CSRF 토큰 요청 완료"));
    return ResponseEntity
        .status(HttpStatus.NO_CONTENT)
        .build();
  }

  @PostMapping("/refresh")
  public ResponseEntity<JwtDto> refresh(
      @CookieValue(value = "REFRESH_TOKEN", required = false) String refreshToken,
      HttpServletResponse response) {
    log.debug("[인증 관리] : 토큰 리프레시 요청 시작");
    JwtInformation jwtInformation = authService.refreshToken(refreshToken);
    ResponseCookie refreshCookie = jwtTokenProvider.generateRefreshTokenCookie(
        jwtInformation.refreshToken());
    response.addHeader("Set-Cookie", refreshCookie.toString());

    JwtDto body = new JwtDto(
        jwtInformation.userDto(),
        jwtInformation.accessToken()
    );

    log.info("[인증 관리] : 토큰 리프레시 요청 완료");
    return ResponseEntity
        .status(HttpStatus.OK)
        .body(body);
  }

  @PostMapping(
      value = "/sign-in",
      consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE}
  )
  public ResponseEntity<JwtDto> signIn(SignInRequest signInRequest,
      HttpServletResponse response) throws JOSEException {
    log.debug("[인증 관리] : 로그인 요청 시작");

    JwtInformation info = authService.signIn(signInRequest.username(), signInRequest.password());

    // refresh 쿠키 설정
    ResponseCookie cookie = jwtTokenProvider.generateRefreshTokenCookie(info.refreshToken());
    response.addHeader("Set-cookie", cookie.toString());

    // FE 로는 access token + user 정보만 보냄
    JwtDto body = new JwtDto(info.userDto(), info.accessToken());

    log.info("[인증 관리] : 로그인 요청 완료");
    return ResponseEntity.ok(body);
  }

  @PostMapping("/sign-out")
  public ResponseEntity<Void> logout(
      @CookieValue(value = "REFRESH_TOKEN", required = false) String refreshToken,
      HttpServletResponse response) {
    log.debug("[인증 관리] : 로그아웃 요청 시작");

    // 쿠키가 없으면 그냥 성공 처리 (클라이언트에서 이미 삭제된 경우)
    if (refreshToken != null && !refreshToken.isBlank()) {
      authService.logout(refreshToken);
    }

    ResponseCookie deleteCookie = jwtTokenProvider.generateRefreshTokenExpirationCookie();
    response.addHeader("Set-Cookie", deleteCookie.toString());

    log.info("[인증 관리] : 로그아웃 요청 완료");
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/reset-password")
  public ResponseEntity<Void> resetPassword(
      @Valid @RequestBody ResetPasswordRequest request
  ) {
    log.debug("[인증 관리] : 비밀번호 초기화 요청 시작");

    String key = "rate-limit:reset:" + request.email();
    if (!rateLimitService.isAllowed(key)) {
      return ResponseEntity.status(429).build(); // Too Many Requests
    }

    passwordResetService.sendTemporaryPassword(request);
    log.info("[인증 관리] : 비밀번호 초기화 요청 완료");
    return ResponseEntity.noContent().build();
  }
}
