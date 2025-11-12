package com.codeit.playlist.domain.user.service.basic;

import com.codeit.playlist.domain.user.dto.data.UserDto;
import com.codeit.playlist.domain.user.dto.request.UserRoleUpdateRequest;
import com.codeit.playlist.domain.user.entity.Role;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.exception.UserNotFoundException;
import com.codeit.playlist.domain.user.mapper.UserMapper;
import com.codeit.playlist.domain.user.repository.UserRepository;
import com.codeit.playlist.domain.user.service.AuthService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class BasicAuthService implements AuthService {

  private final UserRepository userRepository;
  private final UserMapper userMapper;

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  @Transactional
  public UserDto updateRole(UserRoleUpdateRequest request, UUID userId) { // 권한 업데이트 로직, ADMIN 만 가능
    return updateRoleInternal(request, userId);
  }

  @Override
  public UserDto updateRoleInternal(UserRoleUpdateRequest request, UUID userId) {

    User user = userRepository.findById(userId)
        .orElseThrow(() -> UserNotFoundException.withId(userId));

    Role oldRole = user.getRole();
    Role newRole = request.newRole();

    if(!oldRole.equals(newRole)) {
      user.updateRole(newRole);
    }

    //JWT 토큰 확인하는 로직 필요

    return userMapper.toDto(user);
  }
}
