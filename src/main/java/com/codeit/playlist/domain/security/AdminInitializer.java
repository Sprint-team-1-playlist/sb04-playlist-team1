package com.codeit.playlist.domain.security;

import com.codeit.playlist.domain.user.dto.data.UserDto;
import com.codeit.playlist.domain.user.dto.request.UserCreateRequest;
import com.codeit.playlist.domain.user.dto.request.UserRoleUpdateRequest;
import com.codeit.playlist.domain.user.entity.Role;
import com.codeit.playlist.domain.user.service.AuthService;
import com.codeit.playlist.domain.user.service.UserService;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;


@Slf4j
@RequiredArgsConstructor
@Component
public class AdminInitializer implements ApplicationRunner {

  @Value("${playlist.admin.username}")
  private String name;
  @Value("${playlist.admin.password}")
  private String password;
  @Value("${playlist.admin.email}")
  private String email;

  private final AuthService authService;
  private final UserService userService;

  @Override
  public void run(ApplicationArguments args) throws Exception {
    UserCreateRequest request = new UserCreateRequest(name, email, password);
    try {
      UserDto admin = userService.registerUser(request);
      authService.updateRoleInternal(new UserRoleUpdateRequest(Role.ADMIN),
          UUID.fromString(admin.id()));
      log.info("관리자 계정이 성공적으로 생성되었습니다.");
    } catch (RuntimeException e) { // UserAlreadyExistsException 으로 변경할 것
      log.warn("관리자 계정이 이미 존재합니다");
    } catch (Exception e) {
      log.error("관리자 계정 생성 중 오류가 발생했습니다.: {}", e.getMessage());
    }
  }
}
