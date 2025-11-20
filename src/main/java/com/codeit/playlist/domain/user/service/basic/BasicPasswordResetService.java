package com.codeit.playlist.domain.user.service.basic;

import com.codeit.playlist.domain.auth.service.EmailService;
import com.codeit.playlist.domain.user.dto.request.ResetPasswordRequest;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.exception.UserNotFoundException;
import com.codeit.playlist.domain.user.repository.UserRepository;
import com.codeit.playlist.domain.user.service.PasswordResetService;
import com.codeit.playlist.global.redis.TemporaryPasswordStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BasicPasswordResetService implements PasswordResetService {

  private final UserRepository userRepository;
  private final EmailService emailService;
  private final TemporaryPasswordStore tempStore;
  private final PasswordEncoder passwordEncoder;
  private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
  private static final int PASSWORD_LENGTH = 12;
  private static final java.security.SecureRandom RANDOM = new java.security.SecureRandom();

  private static final long TTL_SECONDS = 180;

  @Value("${spring.profiles.active:dev}")
  private String activeProfile;

  @Override
  public void sendTemporaryPassword(ResetPasswordRequest request) {
    log.debug("[사용자 관리] : 임시 비밀번호 발급");

    User user = userRepository.findByEmail(request.email())
        .orElseThrow(() -> UserNotFoundException.withUsername(request.email()));

    String tempPassword = generateRandomPassword();
    String hashedPassword = passwordEncoder.encode(tempPassword);

    if (activeProfile.equals("local") || activeProfile.equals("dev")) {
      log.warn("[DEV ONLY] 임시 비밀번호 (이메일: {}): {}", request.email(), tempPassword);
    }

    //Redis 에 해시된 임시 비밀번호 저장 (3분 TTL)
    tempStore.save(user.getId(), hashedPassword, TTL_SECONDS);

    userRepository.changePassword(user.getId(), hashedPassword);

    //평문 임시 비밀번호를 이메일로 전송
    emailService.sendTemporaryPassword(request.email(), tempPassword);

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
