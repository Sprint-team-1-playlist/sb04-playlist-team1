package com.codeit.playlist.domain.security.oauth;

import com.codeit.playlist.domain.security.PlaylistUserDetails;
import com.codeit.playlist.domain.security.jwt.DbJwtRegistry;
import com.codeit.playlist.domain.security.jwt.JwtInformation;
import com.codeit.playlist.domain.security.jwt.JwtTokenProvider;
import com.codeit.playlist.domain.user.dto.data.UserDto;
import com.codeit.playlist.domain.user.entity.AuthProvider;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.exception.AuthenticationOAuthJwtException;
import com.codeit.playlist.domain.user.exception.EmailAlreadyExistsException;
import com.codeit.playlist.domain.user.mapper.UserMapper;
import com.codeit.playlist.domain.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
  private final DbJwtRegistry jwtRegistry;

  @Value("${management.cookie.secure}")
  private boolean cookieSecure;

  @Value("${oauth2.redirect.base-url:http://localhost:8080}")
  private String frontendBaseUrl;

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
      log.error("[소셜 로그인] : OAuth2 로그인 JWT 생성 실패", e);
      throw AuthenticationOAuthJwtException.withException("JWT 토큰 생성 실패", e.getMessage());
    }

    // JWT 저장 정보 생성
    Instant accessExp = tokenProvider.getExpiryFromToken(accessToken);
    Instant refreshExp = tokenProvider.getExpiryFromToken(refreshToken);

    JwtInformation info = new JwtInformation(
        userDto,
        accessToken, accessExp, now,
        refreshToken, refreshExp, now
    );

    // DB 저장
    jwtRegistry.registerJwtInformation(info);

    // Refresh Token Cookie 생성
    ResponseCookie refreshCookie = ResponseCookie.from("REFRESH_TOKEN", refreshToken)
        .httpOnly(true)
        .secure(cookieSecure) // localhost 환경이므로 false, 배포에서는 true 필수
        .path("/")
        .sameSite("Lax") // <-- 프론트가 같은 도메인(localhost:8080)이므로 Lax가 안정적
        .maxAge(60 * 60 * 24 * 14) // 14일
        .build();

    response.addHeader("Set-Cookie", refreshCookie.toString());

    // Access Token Cookie 생성
    ResponseCookie accessCookie = ResponseCookie.from("ACCESS_TOKEN", accessToken)
        .httpOnly(true)
        .secure(cookieSecure)
        .path("/")
        .sameSite("Lax")
        .maxAge(60 * 30) // 30분
        .build();

    response.addHeader("Set-Cookie", accessCookie.toString());

    //프론트로 Redirect (AccessToken 전달 X)

    String redirect = frontendBaseUrl + "/#/contents";

    log.info("[소셜 로그인] : OAuth2 로그인 성공 -> 사용자 = {}", email);

    response.sendRedirect(redirect);
  }
}
