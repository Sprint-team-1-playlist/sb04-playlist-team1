package com.codeit.playlist.domain.security.oauth;

import com.codeit.playlist.domain.security.jwt.JwtRegistry;
import com.codeit.playlist.domain.security.jwt.JwtTokens;
import com.codeit.playlist.domain.user.entity.AuthProvider;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.exception.EmailAlreadyExistsException;
import com.codeit.playlist.domain.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

  private final UserRepository userRepository;
  private final JwtRegistry jwtRegistry;

  @Value("${playlist.jwt.cookie.secure}")
  private boolean cookieSecure;

  @Value("${oauth2.redirect.base-url:http://localhost:8080}")
  private String frontendBaseUrl;

  @Value("${playlist.jwt.refresh-token.expiration-ms}")
  private long refreshTokenExpirationMs;

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request,
      HttpServletResponse response,
      Authentication authentication) throws IOException {

    CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();

    String email = oAuth2User.getEmail();
    String name = oAuth2User.getName();
    String imageUrl = oAuth2User.getProfileImageUrl();
    String provider = oAuth2User.getProvider();

    // 사용자 조회 또는 OAuth 전용 신규 생성
    AuthProvider authProvider = AuthProvider.valueOf(provider.toUpperCase());
    User user = userRepository.findByEmailAndProvider(email, authProvider)
        .orElseGet(() -> {
          // 기존 로컬 계정 존재 여부 확인
          if (userRepository.findByEmail(email).isPresent()) {
            throw EmailAlreadyExistsException.withEmail(email);
          }
          log.debug("[소셜 로그인] : 신규 소셜 사용자 생성 = {}", email);
          return userRepository.save(User.createOAuthUser(name, email, imageUrl, authProvider));
        });

    JwtTokens tokens = jwtRegistry.issueNewTokensAndInvalidateOld(user);

    // Refresh Token Cookie 생성
    ResponseCookie refreshCookie = ResponseCookie.from("REFRESH_TOKEN", tokens.refreshToken())
        .httpOnly(true)
        .secure(cookieSecure)
        .path("/")
        .sameSite("Lax")
        .maxAge(refreshTokenExpirationMs / 1000) // 7일
        .build();

    response.addHeader("Set-Cookie", refreshCookie.toString());

    String redirect =
        frontendBaseUrl +
            "/#/contents";

    log.info("[소셜 로그인] : OAuth2 로그인 성공 -> 사용자 = {}", email);

    response.sendRedirect(redirect);

  }
}
