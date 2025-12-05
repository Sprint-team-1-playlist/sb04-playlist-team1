package com.codeit.playlist.domain.sse.repository;

import com.codeit.playlist.domain.sse.exception.InvalidSseEmitterException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Repository
public class SseEmitterRepository {

  private final ConcurrentMap<UUID, SseEmitter> data = new ConcurrentHashMap<>();

  public SseEmitter save(UUID receiverId, SseEmitter sseEmitter) {
    if (receiverId == null || sseEmitter == null) {
      throw InvalidSseEmitterException.withId(receiverId, sseEmitter);
    }
    SseEmitter old = data.put(receiverId, sseEmitter);
    if (old != null) {
      try {
        old.complete();
      } catch (Exception ignored) {}
    }

    sseEmitter.onCompletion(() -> data.remove(receiverId, sseEmitter));
    sseEmitter.onTimeout(() -> data.remove(receiverId, sseEmitter));
    sseEmitter.onError(e -> data.remove(receiverId, sseEmitter));

    return sseEmitter;
  }

  public Optional<SseEmitter> findByReceiverId(UUID receiverId) {
    if (receiverId == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(data.get(receiverId));
  }

  public Map<UUID, SseEmitter> findAllByReceiverIdsIn(Collection<UUID> receiverIds) {
    if (receiverIds == null || receiverIds.isEmpty()) {
      return Map.of();
    }
    return data.entrySet().stream()
        .filter(entry -> receiverIds.contains(entry.getKey()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  public Collection<SseEmitter> findAll(){
    return data.values();
  }

  public void delete(UUID receiverId) {
    SseEmitter emitter = data.remove(receiverId);
    if (emitter != null) {
      try {
        emitter.complete();
      } catch (Exception ignored) {}
    }
  }
}
