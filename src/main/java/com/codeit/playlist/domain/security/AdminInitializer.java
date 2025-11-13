package com.codeit.playlist.domain.security;

import com.codeit.playlist.domain.user.dto.data.UserDto;
import com.codeit.playlist.domain.user.dto.request.UserCreateRequest;
import com.codeit.playlist.domain.user.dto.request.UserRoleUpdateRequest;
import com.codeit.playlist.domain.user.entity.Role;
import com.codeit.playlist.domain.user.exception.EmailAlreadyExistsException;
import com.codeit.playlist.domain.user.exception.RequestInValidUuidFormat;
import com.codeit.playlist.domain.auth.service.AuthService;
import com.codeit.playlist.domain.user.service.UserService;
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

  // .env 파일에 개인이 사용할 환경변수 선언, CI/CD 배포 구축 후에 github secret 에 추후 수정할 것

  @Value("${ADMIN_USER}")
  private String name;
  @Value("${ADMIN_PASSWORD}")
  private String password;
  @Value("${ADMIN_EMAIL}")
  private String email;

  private final AuthService authService;
  private final UserService userService;

  @Override
  public void run(ApplicationArguments args) throws Exception {
    log.debug("[사용자 관리] 관리자 계정 생성 시작");
    UserCreateRequest request = new UserCreateRequest(name, email, password);
    try {
      UserDto admin = userService.registerUser(request);
      UUID adminId;
      try{
        adminId = UUID.fromString(admin.id());
              } catch (RequestInValidUuidFormat e) {
                log.error("유효하지 않은 UUID 형식입니다: {}", admin.id());
                throw e;
              }
      authService.updateRoleInternal(new UserRoleUpdateRequest(Role.ADMIN),
          adminId);
      log.info("관리자 계정이 성공적으로 생성되었습니다.");
    } catch (EmailAlreadyExistsException e) {
      log.warn("관리자 계정이 이미 존재합니다");
    } catch (Exception e) {
      log.error("관리자 계정 생성 중 오류가 발생했습니다.: {}", e.getMessage());
      throw e;
    }
  }
}
