package com.codeit.playlist.domain.user.service.basic;

import com.codeit.playlist.domain.user.dto.request.ResetPasswordRequest;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.exception.UserNotFoundException;
import com.codeit.playlist.domain.user.repository.UserRepository;
import com.codeit.playlist.domain.user.service.PasswordResetService;
import com.codeit.playlist.global.redis.TemporaryPasswordStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BasicPasswordResetService implements PasswordResetService {

  private final UserRepository userRepository;
  private final TemporaryPasswordStore tempStore;

  private static final String TEMP_PASSWORD = "temporary1!!";
  private static final long TTL_SECONDS = 180;

  @Override
  public void sendTemporaryPassword(ResetPasswordRequest request) {
    log.debug("[사용자 관리] : 임시 비밀번호 발급");

    User user = userRepository.findByEmail(request.email())
        .orElseThrow(() -> UserNotFoundException.withUsername(request.email()));

    //Redis 에 저장 (3분 TTL)
    tempStore.save(user.getId(), TEMP_PASSWORD, TTL_SECONDS);
    log.info("[사용자 관리] : 임시 비밀번호 발급 완료");
  }
}
