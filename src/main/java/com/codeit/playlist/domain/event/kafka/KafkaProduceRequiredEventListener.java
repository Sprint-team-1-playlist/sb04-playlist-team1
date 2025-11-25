package com.codeit.playlist.domain.event.kafka;

import com.codeit.playlist.domain.event.message.DirectMessageSentEvent;
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

  @Async("eventTaskExcutor")
  @TransactionalEventListener
  public void on(DirectMessageSentEvent event) {
    sendToKafka(event);
  }

  private <T> void sendToKafka(T event) {
    try {
      String payload = objectMapper.writeValueAsString(event);
      kafkaTemplate.send("discodeit.".concat(event.getClass().getSimpleName()), payload);
    } catch (JsonProcessingException e) {
      log.error("Failed to send event to Kafka", e);
      throw new RuntimeException(e);
    }
  }
}
