package com.codeit.playlist.domain.user.service.basic;

import com.codeit.playlist.domain.user.dto.request.ResetPasswordRequest;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.exception.UserNotFoundException;
import com.codeit.playlist.domain.user.repository.UserRepository;
import com.codeit.playlist.domain.user.service.PasswordResetService;
import com.codeit.playlist.global.redis.TemporaryPasswordStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BasicPasswordResetService implements PasswordResetService {

  private final UserRepository userRepository;
  private final TemporaryPasswordStore tempStore;
  private final PasswordEncoder passwordEncoder;
  private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
  private static final int PASSWORD_LENGTH = 12;
  private static final java.security.SecureRandom RANDOM = new java.security.SecureRandom();

  private static final long TTL_SECONDS = 180;

  @Override
  public void sendTemporaryPassword(ResetPasswordRequest request) {
    log.debug("[사용자 관리] : 임시 비밀번호 발급");

    User user = userRepository.findByEmail(request.email())
        .orElseThrow(() -> UserNotFoundException.withUsername(request.email()));

    String tempPassword = generateRandomPassword();
    String hashedPassword = passwordEncoder.encode(tempPassword);
    //Redis 에 저장 (3분 TTL)
    tempStore.save(user.getId(), hashedPassword, TTL_SECONDS);

    // TODO: 이메일로 tempPassword (평문) 전송
    // 주의: hashedPassword가 아닌 tempPassword를 전송해야 함

    log.info("[사용자 관리] : 임시 비밀번호 발급 완료");
  }

  private String generateRandomPassword() {
    StringBuilder password = new StringBuilder(PASSWORD_LENGTH);
    for (int i = 0; i < PASSWORD_LENGTH; i++) {
      password.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
    }
    return password.toString();
  }
}
