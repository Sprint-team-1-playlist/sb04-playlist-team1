package com.codeit.playlist.domain.user.event;

import com.codeit.playlist.domain.auth.service.EmailService;
import com.codeit.playlist.domain.user.dto.data.TempPasswordIssuedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class TempPasswordEventHandler {

  private final EmailService emailService;

  @Async("mailExecutor")
  @TransactionalEventListener(
      classes = TempPasswordIssuedEvent.class,
      phase = TransactionPhase.AFTER_COMMIT
  )
  public void handle(TempPasswordIssuedEvent event) {
    log.info("📩 이메일 발송 핸들러 실행 → {}", event.email());
    sendEmailWithRetry(event);
  }

  @Retryable(
      retryFor = MailException.class,
      maxAttempts = 3,
      backoff = @Backoff(delay = 2000, multiplier = 2)
  )
  public void sendEmailWithRetry(TempPasswordIssuedEvent event) {
    emailService.sendTemporaryPassword(event.email(), event.tempPassword());
  }

  @Recover
  public void recover(MailException e, TempPasswordIssuedEvent event) {
    log.error("❌ 이메일 발송 실패 → {} / 3회 재시도 후 포기", event.email(), e);
  }
}

