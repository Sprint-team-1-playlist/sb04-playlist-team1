package com.codeit.playlist.domain.user.service.basic;

import com.codeit.playlist.domain.auth.exception.AuthAccessDeniedException;
import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.file.S3Uploader;
import com.codeit.playlist.domain.file.exception.FileTooLargeException;
import com.codeit.playlist.domain.file.exception.InvalidImageContentException;
import com.codeit.playlist.domain.file.exception.InvalidImageTypeException;
import com.codeit.playlist.domain.security.PlaylistUserDetails;
import com.codeit.playlist.domain.security.jwt.JwtRegistry;
import com.codeit.playlist.domain.user.dto.data.UserDto;
import com.codeit.playlist.domain.user.dto.request.ChangePasswordRequest;
import com.codeit.playlist.domain.user.dto.request.UserCreateRequest;
import com.codeit.playlist.domain.user.dto.request.UserLockUpdateRequest;
import com.codeit.playlist.domain.user.dto.request.UserUpdateRequest;
import com.codeit.playlist.domain.user.dto.response.CursorResponseUserDto;
import com.codeit.playlist.domain.user.entity.Role;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.exception.*;
import com.codeit.playlist.domain.user.mapper.UserMapper;
import com.codeit.playlist.domain.user.repository.UserRepository;
import com.codeit.playlist.domain.user.repository.UserRepositoryCustom;
import com.codeit.playlist.domain.user.service.UserService;
import com.codeit.playlist.global.constant.S3Properties;
import com.codeit.playlist.global.redis.TemporaryPasswordStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
  private final S3Uploader s3Uploader;
  private final S3Properties s3Properties;

  private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png");
  private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

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

  @Override
  public void updateUserLocked(UUID userId, UserLockUpdateRequest request) {
    log.debug("[사용자 관리] 잠금상태 변경 시작 : userId = {}", userId);

    User user = userRepository.findById(userId)
        .orElseThrow(() -> UserNotFoundException.withId(userId));

    if(request.locked() == user.isLocked()) {
      throw UserLockStateUnchangedException.withId(userId);
    }

    userRepository.updateUserLocked(userId, request.locked());

    jwtRegistry.invalidateJwtInformationByUserId(userId);

    log.info("[사용자 관리] 잠금상태 변경 완료 : userId = {}", userId);

  }

  @Override
  public UserDto updateUser(UUID userId, UserUpdateRequest request, MultipartFile image, Authentication authentication) {
    log.debug("[프로필 관리] 프로필 변경 시작 : userId = {}", userId);

    String currentUserEmail = authentication.getName();
    User currentUser = userRepository.findByEmail(currentUserEmail)
        .orElseThrow(() -> UserNotFoundException.withUsername(currentUserEmail));

    User user = userRepository.findById(userId)
            .orElseThrow(() -> UserNotFoundException.withId(userId));

    if (!currentUser.getId().equals(user.getId()) && !authentication.getAuthorities().stream()
        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
      throw UserProfileAccessDeniedException.withId(userId);
    }

    if (request.name() == null || request.name().isBlank()) {
      throw new UserNameRequiredException();
    }
    user.updateUsername(request.name());

    String oldProfileImageUrl = user.getProfileImageUrl();

    if (image != null && !image.isEmpty()) {
      String contentType = image.getContentType();
      if (!ALLOWED_IMAGE_TYPES.contains(contentType)) {
        throw InvalidImageTypeException.withType(contentType);
      }

      if (image.getSize() > MAX_FILE_SIZE) {
        throw FileTooLargeException.withSize(image.getSize());
      }

      try (InputStream is = image.getInputStream()) {
        byte[] header = new byte[4];
        if (is.read(header) != 4) {
          throw InvalidImageContentException.defaultError();
        }

        boolean isJpeg = header[0] == (byte) 0xFF && header[1] == (byte) 0xD8;
        boolean isPng = header[0] == (byte) 0x89 && header[1] == (byte) 0x50;

        if (!isJpeg && !isPng) {
          throw InvalidImageContentException.defaultError();
        }
      } catch (IOException e) {
        throw InvalidImageContentException.defaultError();
      }

      String extension = contentType.equals("image/png") ? ".png" : ".jpg";
      String key = UUID.randomUUID() + extension;

      String imageUrl = s3Uploader.upload(
          s3Properties.getProfileBucket(),
          key,
          image
      );

      user.updateProfileImageUrl(imageUrl);

      if (oldProfileImageUrl != null) {
        String oldKey = extractKeyFromUrl(oldProfileImageUrl);
        if (oldKey != null) {
          try {
            s3Uploader.delete(s3Properties.getProfileBucket(), oldKey);
          } catch (RuntimeException e) {
            log.warn("[프로필 관리] 기존 프로필 이미지 삭제 실패 : userId = {}, key = {}", userId, oldKey, e);
          }
        }
      }
    }

    UserDto userDto = userMapper.toDto(user);
    log.info("[프로필 관리] 프로필 변경 완료 : userId = {}", userId);

    return userDto;
  }

  private String extractKeyFromUrl(String url) {
    int idx = url.lastIndexOf('/');
    return idx != -1 ? url.substring(idx + 1) : null;
  }
}
