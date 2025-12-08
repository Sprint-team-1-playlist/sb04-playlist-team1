package com.codeit.playlist.domain.message.event.listener;

import com.codeit.playlist.domain.message.event.message.DirectMessageSentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class DirectMessageEventListener {
  private final SimpMessagingTemplate messagingTemplate;

  @Async("websocketExecutor")
  @TransactionalEventListener
  public void on(DirectMessageSentEvent event) {
    String destination = "/sub/conversations/" + event.conversationId() + "/direct-messages";
    messagingTemplate.convertAndSend(destination, event.message());
    log.debug("[Message] DM WebSocket 전송 완료: {}", destination);
  }
}
