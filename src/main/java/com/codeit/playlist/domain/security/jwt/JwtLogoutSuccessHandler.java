package com.codeit.playlist.domain.security.jwt;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtLogoutSuccessHandler implements LogoutSuccessHandler {

  private final JwtRegistry jwtRegistry;

  @Override
  public void onLogoutSuccess(
      HttpServletRequest request,
      HttpServletResponse response,
      Authentication authentication
  ) throws IOException {

    String refresh = extractRefreshTokenFromCookie(request);

    if (refresh != null) {
      jwtRegistry.revokeRefreshToken(refresh);
    }

    // 쿠키 삭제
    Cookie delete = new Cookie("REFRESH_TOKEN", null);
    delete.setPath("/");
    delete.setHttpOnly(true);
    delete.setMaxAge(0);
    response.addCookie(delete);

    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
  }

  private String extractRefreshTokenFromCookie(HttpServletRequest request) {
    if (request.getCookies() == null) return null;
    for (Cookie cookie : request.getCookies()) {
      if ("REFRESH_TOKEN".equals(cookie.getName())) {
        return cookie.getValue();
      }
    }
    return null;
  }
}
