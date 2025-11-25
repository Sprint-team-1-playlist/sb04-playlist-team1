package com.codeit.playlist.domain.event.listener;

import com.codeit.playlist.domain.event.message.DirectMessageSentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class DirectMessageEventListener {
  private final SimpMessagingTemplate messagingTemplate;

  @TransactionalEventListener
  public void handleDirectMessageSent(DirectMessageSentEvent event) {
    String destination = "/sub/conversations/" + event.conversationId() + "/direct-messages";
    messagingTemplate.convertAndSend(destination, event.message());
    log.debug("[Message] DM WebSocket 전송 완료: {}", destination);
  }
}
