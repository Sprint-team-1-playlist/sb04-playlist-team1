package com.codeit.playlist.domain.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider tokenProvider;
  private final UserDetailsService userDetailsService;
  private final JwtRegistry jwtRegistry;
  private final ObjectMapper objectMapper;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    String uri = request.getRequestURI();

    if (isExcludedPath(uri)) {
      filterChain.doFilter(request, response);
      return;
    }

    try {
      String token = resolveToken(request);

      if (StringUtils.hasText(token)) {
        if (tokenProvider.validateAccessToken(token) && jwtRegistry.hasActiveJwtInformationByAccessToken(
            token)) {
          String username = tokenProvider.getUsernameFromToken(token);

          UserDetails userDetails = userDetailsService.loadUserByUsername(username);

          UsernamePasswordAuthenticationToken authentication =
              new UsernamePasswordAuthenticationToken(
                  userDetails,
                  null,
                  userDetails.getAuthorities()
              );

          authentication.setDetails(
              new WebAuthenticationDetailsSource().buildDetails(request)
          );

          SecurityContextHolder.getContext().setAuthentication(authentication);
          log.debug("Set authentication for user: {}", username);
        } else {
          log.debug("Invalid JWT token");
          sendErrorResponse(response, "Invalid JWT token", HttpServletResponse.SC_UNAUTHORIZED);
          return;
        }
      }
    } catch (Exception e) {
      log.debug("JWT authentication failed: {}", e.getMessage());
      SecurityContextHolder.clearContext();
      sendErrorResponse(response, "JWT authentication failed", HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }

    filterChain.doFilter(request, response);
  }

  private String resolveToken(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7);
    }
    return null;
  }

  private void sendErrorResponse(HttpServletResponse response, String message, int status)
      throws IOException {

    response.setStatus(status);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");

    Map<String, Object> body = new HashMap<>();
    body.put("error", message);  // 메시지 넣기

    String jsonResponse = objectMapper.writeValueAsString(body);

    response.getWriter().write(jsonResponse);
  }

  // JWT 인증을 제외할 URL 정의
  private boolean isExcludedPath(String uri) {

    return uri.startsWith("/api/auth/sign-in") ||
        uri.startsWith("/api/auth/sign-up") ||
        uri.startsWith("/api/auth/refresh") ||
        uri.startsWith("/api/auth/logout") ||

        uri.startsWith("/api/users") ||

        uri.startsWith("/api/sse") ||
        uri.equals("/") ||
        uri.equals("/index.html") ||
        uri.equals("/vite.svg") ||
        uri.startsWith("/assets/");
  }
}
