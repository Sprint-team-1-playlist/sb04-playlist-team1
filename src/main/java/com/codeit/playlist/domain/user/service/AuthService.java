package com.codeit.playlist.domain.user.service;

import com.codeit.playlist.domain.user.dto.data.UserDto;
import com.codeit.playlist.domain.user.dto.request.UserRoleUpdateRequest;
import java.util.UUID;

public interface AuthService {

  UserDto updateRole(UserRoleUpdateRequest request, UUID userId);

  UserDto updateRoleInternal(UserRoleUpdateRequest request, UUID userId);
}
