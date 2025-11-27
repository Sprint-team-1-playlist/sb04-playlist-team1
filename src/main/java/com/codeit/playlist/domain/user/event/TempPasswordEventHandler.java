package com.codeit.playlist.domain.user.event;

import com.codeit.playlist.domain.auth.service.EmailService;
import com.codeit.playlist.domain.user.dto.data.TempPasswordIssuedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
@EnableRetry
public class TempPasswordEventHandler {

  private final EmailService emailService;

  @Retryable(
      value = {
          MailException.class},
      retryFor = MailException.class,
      maxAttempts = 3,
      backoff = @Backoff(delay = 2000, multiplier = 2)
  )
  @Async("mailExecutor")
  @TransactionalEventListener(
      classes = TempPasswordIssuedEvent.class,
      phase = TransactionPhase.AFTER_COMMIT
  )
  public void handle(TempPasswordIssuedEvent event) {
    try {
      emailService.sendTemporaryPassword(event.email(), event.tempPassword());
    } catch (Exception e) {
      log.error("[메일] : 임시 비밀번호 전송 실패", e);
    }
    emailService.sendTemporaryPassword(event.email(), event.tempPassword());
  }
}
