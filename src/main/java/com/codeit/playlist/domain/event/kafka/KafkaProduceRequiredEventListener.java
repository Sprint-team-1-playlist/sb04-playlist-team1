package com.codeit.playlist.domain.event.kafka;

import com.codeit.playlist.domain.message.dto.data.DirectMessageDto;
import com.codeit.playlist.domain.notification.dto.data.NotificationDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
@Component
public class KafkaProduceRequiredEventListener {

  private final KafkaTemplate<String, String> kafkaTemplate;
  private final ObjectMapper objectMapper;

  @Async("eventTaskExecutor")
  @TransactionalEventListener
  public void onDirectMessageEvent(DirectMessageDto dm) {
    sendToKafka("playlist.DirectMessageDto", dm);
  }

  @Async("eventTaskExecutor")
  @TransactionalEventListener
  public void onNotificationEvent(NotificationDto notification) {
    sendToKafka("playlist.NotificationDto", notification);
  }

  private <T> void sendToKafka(String topic, T event) {
    try {
      String payload = objectMapper.writeValueAsString(event);
      kafkaTemplate.send(topic, payload);
    } catch (JsonProcessingException e) {
      log.error("[Kafka] DTO 직렬화 실패", e);
      throw new RuntimeException(e);
    }
  }
}
