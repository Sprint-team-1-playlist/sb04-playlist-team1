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
    log.info("[임시 비밀번호] : 메일 전송 시도 → {}", event.email());
    emailService.sendTemporaryPassword(event.email(), event.tempPassword());
  }

  @Recover
  public void recover(MailException e, TempPasswordIssuedEvent event) {
    log.error("[임시 비밀번호] : 메일 전송 3회 실패 → {}", event.email(), e);
  }

}
