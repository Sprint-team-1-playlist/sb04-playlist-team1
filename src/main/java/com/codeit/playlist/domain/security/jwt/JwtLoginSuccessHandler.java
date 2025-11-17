package com.codeit.playlist.domain.security.jwt;

import com.codeit.playlist.domain.security.PlaylistUserDetails;
import com.codeit.playlist.domain.user.dto.data.JwtDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtLoginSuccessHandler implements AuthenticationSuccessHandler {

  private final ObjectMapper objectMapper;
  private final JwtTokenProvider tokenProvider;
  private final JwtRegistry jwtRegistry;

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request,
      HttpServletResponse response,
      Authentication authentication) throws IOException {

    response.setCharacterEncoding("UTF-8");
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);

    if (!(authentication.getPrincipal() instanceof PlaylistUserDetails userDetails)) {
      writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
          "Authentication failed: Invalid user details");
      return;
    }

    String accessToken;
    Instant accessTokenExpiresAt;

    String refreshToken;
    Instant refreshTokenExpiresAt;

    Instant accessIssuedAt = Instant.now();
    Instant refreshIssuedAt = Instant.now();

    try {
      // Access Token
      accessToken = tokenProvider.generateAccessToken(userDetails, accessIssuedAt);
      accessTokenExpiresAt = tokenProvider.getExpiryFromToken(accessToken);

      // Refresh Token
      refreshToken = tokenProvider.generateRefreshToken(userDetails, refreshIssuedAt);
      refreshTokenExpiresAt = tokenProvider.getExpiryFromToken(refreshToken);

    } catch (JOSEException e) {
      log.error("JWT generation failed for {}", userDetails.getUsername(), e);
      writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Token generation failed");
      return;
    }

    jwtRegistry.invalidateJwtInformationByUserId(userDetails.getUserDto().id());

    JwtInformation jwtInformation = new JwtInformation(
        userDetails.getUserDto(),
        accessToken,
        accessTokenExpiresAt,
        accessIssuedAt,
        refreshToken,
        refreshTokenExpiresAt,
        refreshIssuedAt
    );

    jwtRegistry.registerJwtInformation(jwtInformation);

    Cookie refreshCookie = tokenProvider.generateRefreshTokenCookie(refreshToken);
    response.addCookie(refreshCookie);

    JwtDto jwtDto = new JwtDto(
        userDetails.getUserDto(),
        accessToken
    );

    response.setStatus(HttpServletResponse.SC_OK);
    response.getWriter().write(objectMapper.writeValueAsString(jwtDto));


    log.info("Tokens issued for user: {}", userDetails.getUsername());
  }

  private void writeError(HttpServletResponse response, int status, String message) throws IOException {
    response.setStatus(status);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");

    Map<String, Object> body = new HashMap<>();
    body.put("error", message);  // 메시지 넣기

    String jsonResponse = objectMapper.writeValueAsString(body);

    response.getWriter().write(jsonResponse);
  }
}