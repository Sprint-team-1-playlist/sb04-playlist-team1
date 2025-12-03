package com.codeit.playlist.domain.security.oauth;

import com.codeit.playlist.domain.user.repository.UserRepository;
import java.util.HashMap;
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

  private final UserRepository userRepository;

  @Override
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

    OAuth2User oAuth2User = super.loadUser(userRequest);

    String registrationId = userRequest.getClientRegistration().getRegistrationId();
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
    Map<String, Object> kakaoAccount = (Map<String, Object>) oAuth2User.getAttributes().get("kakao_account");
    Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

    Map<String, Object> merged = new HashMap<>(oAuth2User.getAttributes());

    merged.put("email", profile.get("email"));
    merged.put("name", profile.get("nickname"));
    merged.put("picture", profile.get("profile_image_url"));

    return new CustomOAuth2User(merged, userId);
  }

  private CustomOAuth2User mapGoogleUser(OAuth2User oAuth2User, UUID userId) {

    // 구글 attributes는 평평(flat)한 구조
    Map<String, Object> original = oAuth2User.getAttributes();

    String email = (String) original.get("email");
    String name = (String) original.get("name");
    String picture = (String) original.get("picture");

    // attributes 통합 (표준 필드 강제 세팅)
    Map<String, Object> merged = new HashMap<>(original);
    merged.put("email", email);
    merged.put("name", name);
    merged.put("picture", picture);

    return new CustomOAuth2User(merged, userId);
  }
}
