package com.codeit.playlist.domain.user.service;

import com.codeit.playlist.domain.user.dto.data.UserDto;
import com.codeit.playlist.domain.user.dto.request.ChangePasswordRequest;
import com.codeit.playlist.domain.user.dto.request.UserCreateRequest;
import java.util.UUID;

public interface UserService {

  UserDto registerUser(UserCreateRequest request);

  UserDto find(UUID userId);

  void changePassword(UUID userId, ChangePasswordRequest request) ;
}
