package com.codeit.playlist.domain.auth.controller;

import com.codeit.playlist.domain.auth.service.AuthService;
import com.codeit.playlist.domain.jwt.JwtTokenProvider;
import com.codeit.playlist.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;
  private final UserService userService;
  private final JwtTokenProvider jwtTokenProvider;

}
