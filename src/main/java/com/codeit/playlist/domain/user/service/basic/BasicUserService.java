package com.codeit.playlist.domain.user.service.basic;

import com.codeit.playlist.domain.security.PlaylistUserDetails;
import com.codeit.playlist.domain.security.jwt.JwtRegistry;
import com.codeit.playlist.domain.user.dto.data.UserDto;
import com.codeit.playlist.domain.user.dto.request.ChangePasswordRequest;
import com.codeit.playlist.domain.user.dto.request.UserCreateRequest;
import com.codeit.playlist.domain.user.entity.Role;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.exception.EmailAlreadyExistsException;
import com.codeit.playlist.domain.user.exception.UserNotFoundException;
import com.codeit.playlist.domain.user.mapper.UserMapper;
import com.codeit.playlist.domain.user.repository.UserRepository;
import com.codeit.playlist.domain.user.service.UserService;
import java.nio.file.AccessDeniedException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
  private final PasswordEncoder passwordEncoder;
  private final UserMapper userMapper;
  private final JwtRegistry jwtRegistry;

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
    //auth 구현하면서 USER 와 ADMIN 관련 역할 부여할때 수정 예정
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
  public void changePassword(UUID userId, ChangePasswordRequest request)
      throws AccessDeniedException {
    log.debug("[사용자 관리] 패스워드 변경 시작 : userId = {}", userId);

    PlaylistUserDetails principal = (PlaylistUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    UUID loginUserId = principal.getUserDto().id();

    if(!loginUserId.equals(userId)){
      throw new AccessDeniedException("본인의 비밀번호만 변경할 수 있습니다.");
    }

    User user = userRepository.findById(userId).orElseThrow(() -> UserNotFoundException.withId(userId));

    String encodedPassword = passwordEncoder.encode(request.password());
    user.updatePassword(encodedPassword);
    userRepository.save(user);

    jwtRegistry.invalidateJwtInformationByUserId(userId);

    log.info("[사용자 관리] 패스워드 변경 완료 : userId = {}", userId);
    }
  }
