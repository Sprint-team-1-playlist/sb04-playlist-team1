package com.codeit.playlist.domain.user.controller;

import com.codeit.playlist.domain.auth.service.AuthService;
import com.codeit.playlist.domain.user.dto.data.UserDto;
import com.codeit.playlist.domain.user.dto.request.ChangePasswordRequest;
import com.codeit.playlist.domain.user.dto.request.UserCreateRequest;
import com.codeit.playlist.domain.user.dto.request.UserRoleUpdateRequest;
import com.codeit.playlist.domain.user.service.UserService;
import jakarta.validation.Valid;
import java.nio.file.AccessDeniedException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

  private final UserService userService;
  private final AuthService authService;

  @PostMapping
  public ResponseEntity<UserDto> register(@Valid @RequestBody UserCreateRequest userCreateRequest) {
    log.debug("[사용자 관리] 사용자 등록 시작 : name ={}, email = {}", userCreateRequest.name(), userCreateRequest.email());
    UserDto userDto = userService.registerUser(userCreateRequest);
    log.info("[사용자 관리] 사용자 등록 완료 : name ={}, email = {}", userCreateRequest.name(), userCreateRequest.email());
    return ResponseEntity.status(HttpStatus.CREATED).body(userDto);
  }

  @GetMapping("/{userId}")
  public ResponseEntity<UserDto> find(@PathVariable UUID userId) {
    log.debug("[사용자 관리] 사용자 상세 조회 시작 : id = {} ", userId);
    UserDto user = userService.find(userId);
    log.info("[사용자 관리] 사용자 상세 조회 완료 : id = {} ", userId);
    return ResponseEntity.ok(user);
  }

  @PatchMapping("/{userId}/password")
  public ResponseEntity<Void> changePassword(@PathVariable UUID userId,
      @RequestBody ChangePasswordRequest changePasswordRequest) throws AccessDeniedException {
    log.debug("[사용자 관리] 사용자 패스워드 변경 시작 : id = {} ", userId);
    userService.changePassword(userId, changePasswordRequest);
    log.info("[사용자 관리] 사용자 패스워드 변경 완료 : id = {} ", userId);
    return ResponseEntity.ok().build();
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PatchMapping("/{userId}/role")
  public ResponseEntity<Void> updateRole(@PathVariable UUID userId,
      @RequestBody UserRoleUpdateRequest updateRequest) {
    log.debug("[사용자 관리] 사용자 권한 변경 시작 : id = {}, newRole = {} ", userId, updateRequest.newRole());
    authService.updateRole(updateRequest, userId);
    log.info("[사용자 관리] 사용자 권한 변경 완료 : id = {}, newRole = {} ", userId, updateRequest.newRole());
    return ResponseEntity.ok().build();
  }
}
