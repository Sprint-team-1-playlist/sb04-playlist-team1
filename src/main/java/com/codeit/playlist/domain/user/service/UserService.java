package com.codeit.playlist.domain.user.service;

import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.user.dto.data.UserDto;
import com.codeit.playlist.domain.user.dto.request.ChangePasswordRequest;
import com.codeit.playlist.domain.user.dto.request.UserCreateRequest;
import com.codeit.playlist.domain.user.dto.request.UserLockUpdateRequest;
import com.codeit.playlist.domain.user.dto.response.CursorResponseUserDto;
import java.util.UUID;

public interface UserService {

  UserDto registerUser(UserCreateRequest request);

  UserDto find(UUID userId);

  CursorResponseUserDto findUserList(String email, String roleEqual, Boolean isLocked, String cursor, UUID idAfter, int limit, String sortBy, SortDirection sortDirection);

  void changePassword(UUID userId, ChangePasswordRequest request);

  void updateUserLocked(UUID userId, UserLockUpdateRequest request);
}
