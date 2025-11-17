package com.codeit.playlist.domain.auth.service;

import com.codeit.playlist.domain.security.jwt.JwtInformation;
import com.codeit.playlist.domain.user.dto.data.UserDto;
import com.codeit.playlist.domain.user.dto.request.UserRoleUpdateRequest;
import com.nimbusds.jose.JOSEException;
import java.util.UUID;

public interface AuthService {

  UserDto updateRole(UserRoleUpdateRequest request, UUID userId);

  UserDto updateRoleInternal(UserRoleUpdateRequest request, UUID userId);

  JwtInformation signIn(String username, String password) throws JOSEException;

  JwtInformation refreshToken(String refreshToken);

  void logout(String token);
}
