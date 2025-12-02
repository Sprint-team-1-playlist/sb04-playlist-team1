package com.codeit.playlist.domain.user.service;

import com.codeit.playlist.domain.security.CustomOAuth2User;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOAuth2Service extends DefaultOAuth2UserService {

  private final UserRepository userRepository;

  @Override
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

    OAuth2User oAuth2User = super.loadUser(userRequest);

    String email = oAuth2User.getAttributes().get("email").toString();
    String name = oAuth2User.getAttributes().get("name").toString();

    User user = userRepository.findByEmail(email)
        .orElseGet(() ->
            userRepository.save(
                User.createOAuthUser(name, email)
            )
        );
    return new CustomOAuth2User(oAuth2User.getAttributes(), user.getId());
  }
}
