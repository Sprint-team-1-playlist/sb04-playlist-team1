package com.codeit.playlist.domain.event.kafka;

import com.codeit.playlist.domain.event.message.DirectMessageSentEvent;
import com.codeit.playlist.domain.message.dto.data.DirectMessageDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

  @KafkaListener(topics = "playlist.DirectMessageSentEvent")
  public void onDirectMessageSentEvent(String kafkaEvent) {
    try {
      DirectMessageSentEvent event = objectMapper.readValue(kafkaEvent, DirectMessageSentEvent.class);
      DirectMessageDto directMessageDto = event.message();

      UUID receiverId = directMessageDto.receiver().userId();
      String title = directMessageDto.sender().name();
      String content = directMessageDto.content();

      String eventName = "direct-messages";

      // notification 저장 로직

    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
