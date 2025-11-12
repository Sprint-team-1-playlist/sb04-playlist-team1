package com.codeit.playlist.domain.user.service;

import com.codeit.playlist.domain.user.dto.data.UserDto;
import com.codeit.playlist.domain.user.dto.request.UserCreateRequest;

public interface UserService {

  UserDto registerUser(UserCreateRequest request);

}
