package com.codeit.playlist.domain.message.controller;

import com.codeit.playlist.domain.message.dto.data.DirectMessageDto;
import com.codeit.playlist.domain.message.dto.request.DirectMessageSendRequest;
import com.codeit.playlist.domain.message.service.MessageService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

@Slf4j
@RequiredArgsConstructor
@Validated
@Controller
@MessageMapping("/conversations")
public class MessageController {

  private final MessageService messageService;
  private final SimpMessagingTemplate messagingTemplate;

  @MessageMapping("/{conversationId}/direct-messages")
  public void sendMessage(@DestinationVariable UUID conversationId,
      @Payload DirectMessageSendRequest sendRequest) {
    log.debug("[Message] 메시지 전송 요청: {}", conversationId);

    DirectMessageDto messageDto = messageService.save(conversationId, sendRequest);

    messagingTemplate.convertAndSend(
        "/sub/conversations/" + conversationId + "/direct-messages", messageDto
    );
    log.info("[Message] 메시지 전송 응답: {}", conversationId);
  }
}
