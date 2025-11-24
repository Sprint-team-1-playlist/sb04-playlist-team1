package com.codeit.playlist.domain.sse.service;

import com.codeit.playlist.domain.sse.entity.SseMessage;
import com.codeit.playlist.domain.sse.exception.InvalidEventNameException;
import com.codeit.playlist.domain.sse.exception.SseReconnectFailedException;
import com.codeit.playlist.domain.sse.exception.SseSendFailedException;
import com.codeit.playlist.domain.sse.repository.SseEmitterRepository;
import com.codeit.playlist.domain.sse.repository.SseMessageRepository;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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

    sseEmitterRepository.save(receiverId, sseEmitter);

    Optional.ofNullable(lastEventId)
        .ifPresentOrElse(
            id -> {
              for (SseMessage sseMessage : sseMessageRepository.findAllByEventIdAfterAndReceiverId(id, receiverId)) {
                try {
                  sseEmitter.send(sseMessage.toEvent());
                } catch (Exception e) {
                  log.error("SSE 재전송 실패 receiverId={}, eventId={}", receiverId, sseMessage.getEventId(), e);
                  throw SseSendFailedException.withId(receiverId, sseMessage.getEventId());
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
    sseEmitterRepository.findAllByReceiverIdsIn(receiverIds).forEach(sseEmitter -> {
      try {
        sseEmitter.send(event);
      } catch (Exception e) {
        log.error("SSE send 실패 receiverIds={}, eventName={}", receiverIds, eventName, e);
        throw SseSendFailedException.withId(receiverIds.iterator().next(), message.getEventId());
      }
    });
  }

  public void broadcast(String eventName, Object data) {
    if (eventName == null || eventName.isEmpty()) {
      throw InvalidEventNameException.withEventName(eventName);
    }

    SseMessage message = sseMessageRepository.save(SseMessage.createBroadcast(eventName, data));
    Set<DataWithMediaType> event = message.toEvent();
    sseEmitterRepository.findAll().forEach(sseEmitter -> {
      try {
        sseEmitter.send(event);
      } catch (Exception e) {
        log.error("SSE broadcast 실패 eventName={}", eventName, e);
        throw SseSendFailedException.withId(null, message.getEventId());
      }
    });
  }

  @Scheduled(fixedRate = 1000 * 60 * 5)
  public void cleanUp() {
    sseEmitterRepository.findAll().stream()
        .filter(sseEmitter -> !ping(sseEmitter))
        .forEach(sseEmitter -> {
          throw SseSendFailedException.withId(null, null);
        });
  }

  private boolean ping(SseEmitter sseEmitter) {
    try {
      sseEmitter.send(SseEmitter.event().name("ping").build());
      return true;
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      return false;
    }
  }
}
