package com.codeit.playlist.domain.user.controller;

import com.codeit.playlist.domain.user.dto.data.UserDto;
import com.codeit.playlist.domain.user.dto.request.UserCreateRequest;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.service.UserService;
import com.codeit.playlist.domain.user.service.basic.BasicUserService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

  private final BasicUserService userService;

  @PostMapping
  public ResponseEntity<UserDto> register(@Valid @RequestBody UserCreateRequest userCreateRequest) {
    return ResponseEntity.status(HttpStatus.CREATED).body(userService.registerUser(userCreateRequest));
  }

  @GetMapping("/{userId}")
  public ResponseEntity<UserDto> find(@PathVariable UUID userId) {
    UserDto user = userService.find(userId);
    return ResponseEntity.ok(user);
  }
}
