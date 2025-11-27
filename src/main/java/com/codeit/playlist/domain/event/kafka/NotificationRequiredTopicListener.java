package com.codeit.playlist.domain.event.kafka;

import com.codeit.playlist.domain.message.dto.data.DirectMessageDto;
import com.codeit.playlist.domain.notification.dto.data.NotificationDto;
import com.codeit.playlist.domain.sse.entity.SseMessage;
import com.codeit.playlist.domain.sse.service.SseService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class NotificationRequiredTopicListener {

  private final ObjectMapper objectMapper;
  private final SseService sseService;

  @KafkaListener(topics = "playlist.DirectMessageDto")
  public void onDirectMessageEvent(String kafkaEvent) {
    try {
      DirectMessageDto event = objectMapper.readValue(kafkaEvent, DirectMessageDto.class);

      UUID receiverId = event.receiver().userId();

      // notification 저장 로직

      SseMessage sseMessage = SseMessage.create(
          Set.of(receiverId),
          "direct-messages",
          event
      );

      sseService.send(sseMessage.getReceiverIds(), sseMessage.getEventName(), sseMessage.getEventData());

      log.info("[Notification] DM SSE 전송 완료: receiverId={}, conversationId={}",
          receiverId,
          event.conversationId());

    } catch (JsonProcessingException e) {
      log.error("[Notification] Kafka 메시지에서 DirectMessageDto 변환 실패", e);
      throw new RuntimeException(e);
    }
  }

  @KafkaListener(topics = "playlist.NotificationDto")
  public void onNotificationEvent(String kafkaEvent) {
    try {
      NotificationDto event = objectMapper.readValue(kafkaEvent, NotificationDto.class);

      UUID receiverId = event.receiverId();

      // notification 저장 로직

      SseMessage sseMessage = SseMessage.create(
          Set.of(receiverId),
          "notifications",
          event
      );

      sseService.send(sseMessage.getReceiverIds(), sseMessage.getEventName(), sseMessage.getEventData());

      log.info("[Notification] 알림 SSE 전송 완료: receiverId={}", receiverId);

    } catch (JsonProcessingException e) {
      log.error("[Notification] Kafka 메시지에서 NotificationDto 변환 실패", e);
      throw new RuntimeException(e);
    }
  }
}
