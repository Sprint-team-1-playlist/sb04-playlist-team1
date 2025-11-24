package com.codeit.playlist.domain.sse.service;

import com.codeit.playlist.domain.sse.entity.SseMessage;
import com.codeit.playlist.domain.sse.exception.InvalidEventNameException;
import com.codeit.playlist.domain.sse.exception.SseReconnectFailedException;
import com.codeit.playlist.domain.sse.exception.SseSendFailedException;
import com.codeit.playlist.domain.sse.repository.SseEmitterRepository;
import com.codeit.playlist.domain.sse.repository.SseMessageRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter.DataWithMediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseService {
  @Value("${sse.timeout:300000}")
  private long timeout;

  private final SseEmitterRepository sseEmitterRepository;
  private final SseMessageRepository sseMessageRepository;

  public SseEmitter connect(UUID receiverId, UUID lastEventId) {
    if (receiverId == null) throw SseReconnectFailedException.withId(null, null);

    SseEmitter sseEmitter = new SseEmitter(timeout);

    sseEmitter.onCompletion(() -> sseEmitterRepository.delete(receiverId, sseEmitter));
    sseEmitter.onTimeout(() -> sseEmitterRepository.delete(receiverId, sseEmitter));
    sseEmitter.onError(ex -> sseEmitterRepository.delete(receiverId, sseEmitter));

    Optional.ofNullable(lastEventId)
        .ifPresentOrElse(
            id -> {
              for (SseMessage sseMessage : sseMessageRepository.findAllByEventIdAfterAndReceiverId(id, receiverId)) {
                try {
                  sseEmitter.send(sseMessage.toEvent());
                } catch (Exception e) {
                  log.error("SSE 재전송 실패 receiverId={}, eventId={}", receiverId, sseMessage.getEventId(), e);
                  throw SseReconnectFailedException.withId(receiverId, sseMessage.getEventId());
                }
              }
            },
            () -> {
              if (!ping(sseEmitter)) {
                log.error("Initial ping failed for receiverId={}", receiverId);
                throw SseReconnectFailedException.withId(receiverId, null);
              }
            }
        );
    sseEmitterRepository.save(receiverId, sseEmitter);
    return sseEmitter;
  }

  public void send(Collection<UUID> receiverIds, String eventName, Object data) {
    if (receiverIds == null || receiverIds.isEmpty()) {
      log.warn("send 호출 시 receiverIds가 비어있음");
      return;
    }
    if (eventName == null || eventName.isEmpty()) {
      throw InvalidEventNameException.withEventName(eventName);
    }

    SseMessage message = sseMessageRepository.save(SseMessage.create(receiverIds, eventName, data));
    Set<DataWithMediaType> event = message.toEvent();
    List<UUID> failedReceivers = new ArrayList<>();

    Map<UUID, SseEmitter> emitterMap = sseEmitterRepository.findAllByReceiverIdsIn(receiverIds);

    emitterMap.forEach((id, sseEmitter) -> {
      try {
        sseEmitter.send(event);
      } catch (Exception e) {
        log.error("SSE send 실패 receiverId={}, eventName={}, eventId={}", id, eventName, message.getEventId(), e);
        failedReceivers.add(id);
      }
    });

    if (!failedReceivers.isEmpty()) {
      throw SseSendFailedException.withIds(failedReceivers, message.getEventId());
    }
  }

  public void broadcast(String eventName, Object data) {
    if (eventName == null || eventName.isEmpty()) {
      throw InvalidEventNameException.withEventName(eventName);
    }

    SseMessage message = sseMessageRepository.save(SseMessage.createBroadcast(eventName, data));
    Set<DataWithMediaType> event = message.toEvent();
    AtomicInteger failureCount = new AtomicInteger(0);
    sseEmitterRepository.findAll().forEach(sseEmitter -> {
      try {
        sseEmitter.send(event);
      } catch (Exception e) {
        log.error("SSE broadcast 실패 eventName={}, eventId={}", eventName, message.getEventId(), e);
        failureCount.incrementAndGet();
      }
    });
    if (failureCount.get() > 0) {
      log.error("SSE broadcast 실패 개수={}, eventId={}", failureCount.get(), message.getEventId());
    }
  }

  @Scheduled(fixedRate = 1000 * 60 * 5)
  public void cleanUp() {
    sseEmitterRepository.findAll().stream()
        .filter(sseEmitter -> !ping(sseEmitter))
        .forEach(sseEmitter -> {
          try {
            sseEmitter.complete();
          } catch (Exception e) {
            log.warn("Failed to complete dead emitter during cleanup", e);
          }
        });
  }

  private boolean ping(SseEmitter sseEmitter) {
    try {
      sseEmitter.send(SseEmitter.event().name("ping").build());
      return true;
    } catch (Exception e) {
      log.error("SSE ping 실패", e);
      return false;
    }
  }
}
