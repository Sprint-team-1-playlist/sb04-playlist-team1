package com.codeit.playlist.domain.security.oauth;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

@RequiredArgsConstructor
@Getter
public class CustomOAuth2User implements OAuth2User, Serializable {

  private static final long serialVersionUID = 1L;

  private final UUID userId;
  private final String email;
  private final String name;
  private final String profileImageUrl;
  private final String provider;
  private final Map<String, Object> attributes;

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
    // OAuth2 스펙상 null이면 안 됨
    if (name != null && !name.isBlank()) {
      return name;
    }
    if (email != null && !email.isBlank()) return email;
    return userId.toString();
  }

}
