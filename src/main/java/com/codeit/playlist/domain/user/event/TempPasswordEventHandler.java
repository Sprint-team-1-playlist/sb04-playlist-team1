package com.codeit.playlist.domain.user.event;

import com.codeit.playlist.domain.user.dto.data.TempPasswordIssuedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class TempPasswordEventHandler {

  private final TempPasswordMailSender tempPasswordMailSender;

  @Async("mailExecutor")
  @TransactionalEventListener(
      classes = TempPasswordIssuedEvent.class,
      phase = TransactionPhase.AFTER_COMMIT
  )
  public void handle(TempPasswordIssuedEvent event) {
    log.info("📩 임시 비밀번호 핸들러 실행 → {}", event.email());
    tempPasswordMailSender.sendEmailWithRetry(event);   // ⬅ 여기서는 프록시를 통해 호출됨
  }
}

