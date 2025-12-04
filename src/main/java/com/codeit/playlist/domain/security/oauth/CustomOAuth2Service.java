package com.codeit.playlist.domain.security.oauth;

import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2Service extends DefaultOAuth2UserService {


  @Override
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

    OAuth2User oAuth2User = super.loadUser(userRequest);

    String registrationId = userRequest.getClientRegistration().getRegistrationId().toLowerCase();
    log.info("[소셜 로그인] OAuth2 로그인 시도 Provider = {}", registrationId);

    // 실제 User 엔티티 ID는 OAuth2SuccessHandler에서 생성/조회하기 때문에
    // 여기선 임시 UUID 사용
    UUID tempUserId = UUID.randomUUID();

    return switch (registrationId.toLowerCase()) {
      case "google" -> mapGoogleUser(oAuth2User, tempUserId);
      case "kakao"  -> mapKakaoUser(oAuth2User, tempUserId);
      default       -> throw new IllegalArgumentException("지원하지 않는 OAuth Provider: " + registrationId);
    };
  }

  private CustomOAuth2User mapKakaoUser(OAuth2User oAuth2User, UUID userId) {
    Map<String, Object> attributes = oAuth2User.getAttributes();

    Object accountObj = attributes.get("kakao_account");
    if (!(accountObj instanceof Map)) {
      throw new OAuth2AuthenticationException("카카오 계정 정보를 가져올 수 없습니다.");
    }
    @SuppressWarnings("unchecked")
    Map<String, Object> account = (Map<String, Object>) accountObj;

    String email = account != null ? (String) account.get("email") : null;

    if (email == null || email.isBlank()) {
      throw new OAuth2AuthenticationException("카카오 로그인 시 이메일 제공이 필요합니다.");
    }

    Object profileObj = account.get("profile");
    @SuppressWarnings("unchecked")
        Map<String, Object> profile = profileObj instanceof Map ? (Map<String, Object>) profileObj : null;

    String name = profile != null ? (String) profile.get("nickname") : null;
    String picture = profile != null ? (String) profile.get("profile_image_url") : null;

    return new CustomOAuth2User(
        userId,
        email,
        name,
        picture,
        "kakao",
        oAuth2User.getAttributes()
    );
  }

  private CustomOAuth2User mapGoogleUser(OAuth2User oAuth2User, UUID userId) {

    Map<String, Object> attributes = oAuth2User.getAttributes();

    String email = (String) attributes.get("email");
    String name = (String) attributes.get("name");
    String picture = (String) attributes.get("picture");

    return new CustomOAuth2User(
        userId,
        email,
        name,
        picture,
        "google",
        attributes
    );
  }
}
