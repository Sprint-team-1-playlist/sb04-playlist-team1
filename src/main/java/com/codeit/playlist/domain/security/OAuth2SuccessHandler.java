package com.codeit.playlist.domain.security;

import com.codeit.playlist.domain.security.jwt.JwtTokenProvider;
import com.codeit.playlist.domain.user.dto.data.UserDto;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.mapper.UserMapper;
import com.codeit.playlist.domain.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

  private final JwtTokenProvider tokenProvider;
  private final UserRepository userRepository;
  private final UserMapper userMapper;

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request,
      HttpServletResponse response,
      Authentication authentication) throws IOException {

    CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();

    String email = oAuth2User.getEmail();
    String name = oAuth2User.getName();
    String imageUrl = oAuth2User.getProfileImageUrl();

    // 사용자 조회 또는 OAuth 전용 신규 생성
    User user = userRepository.findByEmail(email)
        .orElseGet(() -> {
          log.info("신규 소셜 사용자 생성: {}", email);
          return userRepository.save(User.createOAuthUser(name, email, imageUrl));
        });

    // User → UserDto 변환 (MapStruct 사용)
    UserDto userDto = userMapper.toDto(user);

    // PlaylistUserDetails 생성
    PlaylistUserDetails userDetails = new PlaylistUserDetails(userDto, null);

    // JWT 발급
    Instant now = Instant.now();
    String accessToken;
    String refreshToken;

    try {
      accessToken = tokenProvider.generateAccessToken(userDetails, now);
      refreshToken = tokenProvider.generateRefreshToken(userDetails, now);
    } catch (Exception e) {
      throw new RuntimeException("OAuth2 로그인 JWT 생성 실패", e);
    }

    // Refresh Token Cookie 생성
    ResponseCookie refreshCookie = tokenProvider.generateRefreshTokenCookie(refreshToken);
    response.addHeader("Set-Cookie", refreshCookie.toString());

    // 프론트로 Redirect (AccessToken 전달)
    String redirect = "http://localhost:8080/#/contents" + "?accessToken=" + accessToken;

    log.info("OAuth2 로그인 성공 → 사용자: {}, Redirect: {}", email, redirect);

    response.sendRedirect(redirect);
  }
}
