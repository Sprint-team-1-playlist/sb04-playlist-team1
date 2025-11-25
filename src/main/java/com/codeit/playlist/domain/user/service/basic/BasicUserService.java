package com.codeit.playlist.domain.user.service.basic;

import com.codeit.playlist.domain.auth.exception.AuthAccessDeniedException;
import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.security.PlaylistUserDetails;
import com.codeit.playlist.domain.security.jwt.JwtRegistry;
import com.codeit.playlist.domain.user.dto.data.UserDto;
import com.codeit.playlist.domain.user.dto.request.ChangePasswordRequest;
import com.codeit.playlist.domain.user.dto.request.UserCreateRequest;
import com.codeit.playlist.domain.user.dto.response.CursorResponseUserDto;
import com.codeit.playlist.domain.user.entity.Role;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.exception.EmailAlreadyExistsException;
import com.codeit.playlist.domain.user.exception.NewPasswordRequired;
import com.codeit.playlist.domain.user.exception.PasswordMustCharacters;
import com.codeit.playlist.domain.user.exception.UserNotFoundException;
import com.codeit.playlist.domain.user.mapper.UserMapper;
import com.codeit.playlist.domain.user.repository.UserRepository;
import com.codeit.playlist.domain.user.repository.UserRepositoryCustom;
import com.codeit.playlist.domain.user.service.UserService;
import com.codeit.playlist.global.redis.TemporaryPasswordStore;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BasicUserService implements UserService {

  private final UserRepository userRepository;
  private final UserRepositoryCustom userRepositoryCustom;
  private final PasswordEncoder passwordEncoder;
  private final UserMapper userMapper;
  private final JwtRegistry jwtRegistry;
  private final TemporaryPasswordStore temporaryPasswordStore;

  @Value("${ADMIN_EMAIL}")
  private String adminEmail;


  @Override
  public UserDto registerUser(UserCreateRequest request) {
    log.debug("[사용자 관리] 사용자 등록 시작 : email = {}", request.email());
    User newUser = userMapper.toEntity(request);

    if (userRepository.existsByEmail(newUser.getEmail())) {
      throw EmailAlreadyExistsException.withEmail(newUser.getEmail());
    }

    if (newUser.getEmail().equals(adminEmail)) {
      newUser.updateRole(Role.ADMIN);
    }
    if (newUser.getRole() == null) {
      newUser.updateRole(Role.USER);
    }

    String encodedPassword = passwordEncoder.encode(newUser.getPassword());
    newUser.updatePassword(encodedPassword);
    userRepository.save(newUser);

    log.info("[사용자 관리] 사용자 등록 완료 : email = {}", request.email());

    UserDto userDto = userMapper.toDto(newUser);
    return userDto;
  }

  @Transactional(readOnly = true)
  @Override
  public UserDto find(UUID userId) {
    log.debug("[사용자 관리] 사용자 조회 시작 : userId = {}", userId);
    UserDto userDto = userRepository.findById(userId)
        .map(userMapper::toDto)
        .orElseThrow(() -> UserNotFoundException.withId(userId));

    log.debug("[사용자 관리] 사용자 조회 완료 : userId = {}", userId);
    return userDto;
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public CursorResponseUserDto findUserList(String emailLike,
      String roleEqual,
      Boolean isLocked,
      String cursor,
      UUID idAfter,
      int limit,
      String sortBy,
      SortDirection sortDirection){
    log.debug("[사용자 관리] 사용자 목록 조회 시작");

    long totalCount = userRepositoryCustom.countUsers(emailLike, roleEqual, isLocked);

    List<User> users = userRepositoryCustom.searchUsers(
        emailLike,
        roleEqual,
        isLocked,
        cursor,
        idAfter,
        limit,
        sortBy,
        sortDirection
    );

    boolean hasNext = users.size() > limit;
    if (hasNext) users = users.subList(0, limit);

    String nextCursor = null;
    UUID nextIdAfter = null;

    if (hasNext) {
      User last = users.get(users.size() - 1);

      // 정렬 기준별로 nextCursor 값 채움
      switch (sortBy) {
        case "name" -> nextCursor = last.getName();
        case "email" -> nextCursor = last.getEmail();
        case "createdAt" -> nextCursor = last.getCreatedAt() != null
            ? last.getCreatedAt().toString() : null;
        case "isLocked" -> nextCursor = String.valueOf(last.isLocked());
        case "role" -> nextCursor = last.getRole() != null
            ? last.getRole().name() : null;
            default -> nextCursor = last.getCreatedAt() != null
            ? last.getCreatedAt().toString() : null;
      }

      nextIdAfter = last.getId();
    }

    List<UserDto> dtos = users.stream().map(userMapper::toDto).toList();

    return new CursorResponseUserDto(
        dtos,
        nextCursor,
        nextIdAfter,
        hasNext,
        totalCount,
        sortBy,
        sortDirection
    );
  }

  @Override
  public void changePassword(UUID userId, ChangePasswordRequest request) {
    log.debug("[사용자 관리] 패스워드 변경 시작 : userId = {}", userId);

    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof PlaylistUserDetails)) {
      throw AuthAccessDeniedException.withId(userId);
    }
    PlaylistUserDetails principal = (PlaylistUserDetails) authentication.getPrincipal();

    UUID loginUserId = principal.getUserDto().id();

    if (!loginUserId.equals(userId)) {
      throw AuthAccessDeniedException.withId(userId);
    }

    if (request.password() == null || request.password().isBlank()) {
      throw NewPasswordRequired.withId(userId);
    }

    if (request.password().length() < 8) {
      throw PasswordMustCharacters.withId(userId);
    }

    String encodedPassword = passwordEncoder.encode(request.password());
    userRepository.changePassword(userId, encodedPassword);

    jwtRegistry.invalidateJwtInformationByUserId(userId);

    temporaryPasswordStore.delete(userId);

    log.info("[사용자 관리] 패스워드 변경 완료 : userId = {}", userId);
  }
}
