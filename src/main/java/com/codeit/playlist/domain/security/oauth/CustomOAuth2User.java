package com.codeit.playlist.domain.security.oauth;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

@RequiredArgsConstructor
public class CustomOAuth2User implements OAuth2User, Serializable {

  private static final long serialVersionUID = 1L;

  private final Map<String, Object> attributes;
  private final UUID userId;

  @Override
  public Map<String, Object> getAttributes() {
    return attributes;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_USER"));
  }

  @Override
  public String getName() {
    return getNameFromAttributes();
  }

  public UUID getUserId() {
    return userId;
  }


  public String getEmail() {
    return (String) attributes.get("email");
  }

  public String getNameFromAttributes() {
    return (String) attributes.get("name");
  }

  public String getProfileImageUrl() {
    return (String) attributes.get("picture"); // 구글은 picture 필드
  }


}
