package com.codeit.playlist.domain.user.service;

import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.user.dto.data.UserDto;
import com.codeit.playlist.domain.user.dto.request.ChangePasswordRequest;
import com.codeit.playlist.domain.user.dto.request.UserCreateRequest;
import com.codeit.playlist.domain.user.dto.request.UserLockUpdateRequest;
import com.codeit.playlist.domain.user.dto.request.UserUpdateRequest;
import com.codeit.playlist.domain.user.dto.response.CursorResponseUserDto;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

  UserDto registerUser(UserCreateRequest request);

  UserDto find(UUID userId);

  CursorResponseUserDto findUserList(String email, String roleEqual, Boolean isLocked, String cursor, UUID idAfter, int limit, String sortBy, SortDirection sortDirection);

  void changePassword(UUID userId, ChangePasswordRequest request);

  void updateUserLocked(UUID userId, UserLockUpdateRequest request);

  UserDto updateUser(UUID userId, UserUpdateRequest request, MultipartFile image);
}
