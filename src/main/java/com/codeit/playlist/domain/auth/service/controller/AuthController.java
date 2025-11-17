package com.codeit.playlist.domain.auth.service.controller;

import com.codeit.playlist.domain.auth.service.AuthService;
import com.codeit.playlist.domain.security.jwt.JwtInformation;
import com.codeit.playlist.domain.security.jwt.JwtRegistry;
import com.codeit.playlist.domain.security.jwt.JwtTokenProvider;
import com.codeit.playlist.domain.user.dto.data.JwtDto;
import com.codeit.playlist.domain.user.service.UserService;
import com.nimbusds.jose.JOSEException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;
  private final UserService userService;
  private final JwtTokenProvider jwtTokenProvider;
  private final JwtRegistry jwtRegistry;

  @GetMapping("/csrf-token")
  public ResponseEntity<Void> getCsrfToken(CsrfToken csrfToken) {
    log.debug("CSRF 토큰 요청");

        if (csrfToken != null) {
            log.info("CSRF 토큰: {}", csrfToken.getToken());
          } else {
            log.trace("CSRF 토큰이 존재하지 않습니다.");
          }

    return ResponseEntity
        .status(HttpStatus.NO_CONTENT)
        .build();
  }

  @PostMapping("/refresh")
  public ResponseEntity<JwtDto> refresh(@CookieValue(value = "REFRESH_TOKEN", required = false) String refreshToken,
      HttpServletResponse response) {
    log.info("토큰 리프레시 요청");
    JwtInformation jwtInformation = authService.refreshToken(refreshToken);
    Cookie refreshCookie = jwtTokenProvider.generateRefreshTokenCookie(
        jwtInformation.refreshToken());
    response.addCookie(refreshCookie);

    JwtDto body = new JwtDto(
        jwtInformation.userDto(),
        jwtInformation.accessToken()
    );
    return ResponseEntity
        .status(HttpStatus.OK)
        .body(body);
  }

  @PostMapping("/sign-in")
  public ResponseEntity<JwtDto> signIn(@RequestParam String username,
      @RequestParam String password,
      HttpServletResponse response) throws JOSEException {

    JwtInformation info = authService.signIn(username, password);

    // refresh 쿠키 설정
    Cookie cookie = jwtTokenProvider.generateRefreshTokenCookie(info.refreshToken());
    response.addCookie(cookie);

    // FE 로는 access token + user 정보만 보냄
    JwtDto body = new JwtDto(info.userDto(), info.accessToken());

    return ResponseEntity.ok(body);
  }

  @PostMapping("/sign-out")
  public ResponseEntity<Void> logout(
      @CookieValue(value = "REFRESH_TOKEN", required = false) String refreshToken,
      HttpServletResponse response) {
    log.info("로그아웃 요청");

    // 쿠키가 없으면 그냥 성공 처리 (클라이언트에서 이미 삭제된 경우)
    if (refreshToken != null && !refreshToken.isBlank()) {
      authService.logout(refreshToken);
    }


    Cookie deleteCookie = jwtTokenProvider.generateRefreshTokenExpirationCookie();
    response.addCookie(deleteCookie);

    return ResponseEntity.noContent().build();
  }

}
