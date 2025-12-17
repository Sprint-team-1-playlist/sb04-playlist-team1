package com.codeit.playlist.domain.sse.repository;

import com.codeit.playlist.domain.sse.entity.SseMessage;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
public class SseMessageRepository {

  @Value("${sse.event-queue-capacity:100}")
  private int eventQueueCapacity;

  private final ConcurrentLinkedDeque<UUID> eventIdQueue = new ConcurrentLinkedDeque<>();
  private final Map<UUID, SseMessage> messages = new ConcurrentHashMap<>();

  public SseMessage save(SseMessage message) {
    makeAvailableCapacity();

    UUID eventId = message.getEventId();
    eventIdQueue.addLast(eventId);
    messages.put(eventId, message);
    return message;
  }

  public List<SseMessage> findAllByEventIdAfterAndReceiverId(UUID eventId, UUID receiverId) {
    return eventIdQueue.stream()
        .filter(Objects::nonNull)
        .dropWhile(id -> eventId != null && !id.equals(eventId))
        .skip(eventId != null ? 1 : 0)
        .map(messages::get)
        .filter(Objects::nonNull) // 동시 삭제 대비
        .filter(msg -> msg.isReceivable(receiverId))
        .collect(Collectors.toList());
  }

  private void makeAvailableCapacity() {
    int capacity = Math.max(1, eventQueueCapacity);
    int availableCapacity = capacity - eventIdQueue.size();
    while (availableCapacity < 1) {
      UUID removedEventId = eventIdQueue.removeFirst();
      messages.remove(removedEventId);
      availableCapacity++;
    }
  }
}
