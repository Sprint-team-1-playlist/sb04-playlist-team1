package com.codeit.playlist.domain.user.event;

import com.codeit.playlist.domain.auth.service.EmailService;
import com.codeit.playlist.domain.user.dto.data.TempPasswordIssuedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TempPasswordMailSender {

  private final EmailService emailService;

  @Retryable(
      retryFor = MailException.class,
      maxAttempts = 3,
      backoff = @Backoff(delay = 2000, multiplier = 2)
  )
  public void sendEmailWithRetry(TempPasswordIssuedEvent event) {
    log.info("[임시 비밀번호] : 메일 전송 시도 -> {}", maskEmail(event.email()));
    emailService.sendTemporaryPassword(event.email(), event.tempPassword());
  }

  @Recover
  public void recover(MailException e, TempPasswordIssuedEvent event) {
    log.error("[임시 비밀번호] : 메일 전송 3회 실패 -> {}",maskEmail(event.email()), e);
    log.warn("메일 미발송 -> 운영자 확인 필요: {}", maskEmail(event.email()));
  }

  private String maskEmail(String email) {
    if (email == null || !email.contains("@")) {
      return "***";
    }
    String[] parts = email.split("@");
    String localPart = parts[0];
    String domain = parts[1];

    if (localPart.length() <= 2) {
      return "***@" + domain;
    }

    return localPart.substring(0, 2) + "***@" + domain;
  }

}
