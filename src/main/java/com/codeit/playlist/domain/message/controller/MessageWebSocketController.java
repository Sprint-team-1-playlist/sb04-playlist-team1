package com.codeit.playlist.domain.message.controller;

import com.codeit.playlist.domain.message.dto.request.DirectMessageSendRequest;
import com.codeit.playlist.domain.message.service.MessageService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

@Slf4j
@RequiredArgsConstructor
@Validated
@Controller
@MessageMapping("/conversations")
public class MessageWebSocketController {

  private final MessageService messageService;

  @MessageMapping("/{conversationId}/direct-messages")
  public void sendMessage(@DestinationVariable UUID conversationId,
      @Payload @Valid DirectMessageSendRequest sendRequest,
      Principal principal) {
    log.debug("[Message] 메시지 전송 요청: {}", conversationId);

    messageService.save(conversationId, sendRequest, principal);

    log.info("[Message] 메시지 전송 응답: {}", conversationId);
  }
}
