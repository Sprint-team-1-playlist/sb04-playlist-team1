package com.codeit.playlist.domain.security;

import com.codeit.playlist.domain.user.dto.data.UserDto;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@EqualsAndHashCode
@Getter
@RequiredArgsConstructor
public class PlaylistUserDetails implements UserDetails, Serializable {
  private static final long serialVersionUID = 1L;

  private final UserDto userDto;
  private final String password;

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + userDto.role().name()));
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getUsername() {
    return userDto.email();
  }
}
