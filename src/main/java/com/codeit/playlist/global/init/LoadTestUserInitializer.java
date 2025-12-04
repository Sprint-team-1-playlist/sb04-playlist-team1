package com.codeit.playlist.global.init;

import com.codeit.playlist.domain.user.dto.data.UserDto;
import com.codeit.playlist.domain.user.dto.request.UserCreateRequest;
import com.codeit.playlist.domain.user.exception.EmailAlreadyExistsException;
import com.codeit.playlist.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class LoadTestUserInitializer implements ApplicationRunner {

  @Value("${loadtest.user.prefix:tester}")
  private String userPrefix;

  @Value("${loadtest.user.count:50}")
  private int userCount;

  @Value("${loadtest.user.password:abcd1234?}")
  private String defaultPassword;

  private final UserService userService;

  @Override
  public void run(ApplicationArguments args) throws Exception {
    log.info("[부하테스트 계정 초기화] {}{} ~ {}{} 생성 시작…",
        userPrefix, 1, userPrefix, userCount);

    for (int i = 1; i <= userCount; i++) {
      String email = userPrefix + i + "@test.com";
      String name = userPrefix + i;

      try {
        UserCreateRequest request = new UserCreateRequest(
            name,
            email,
            defaultPassword
        );

        UserDto created = userService.registerUser(request);
        log.info("[부하테스트 계정 생성] {} ({})", created.name(), created.email());

      } catch (EmailAlreadyExistsException e) {
        log.debug("[부하테스트 계정 존재함] {} — 건너뜀", email);
      } catch (Exception e) {
        log.error("[부하테스트 계정 생성 실패] {} — {}", email, e.getMessage());
        throw e;
      }
    }

    log.info("[부하테스트 계정 초기화 완료]");
  }
}