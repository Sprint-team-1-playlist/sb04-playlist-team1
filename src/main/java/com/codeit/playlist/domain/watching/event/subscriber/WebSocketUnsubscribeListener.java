package com.codeit.playlist.domain.watching.event.subscriber;

import com.codeit.playlist.domain.watching.exception.WatchingNotFoundException;
import com.codeit.playlist.domain.watching.service.WatchingSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketUnsubscribeListener {
    private final WatchingSessionService watchingSessionService;

    @EventListener
    public void onUnsubscribe(SessionUnsubscribeEvent event) {
        sendToService(event);
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        sendToService(event);
    }

    private void sendToService(Object event) {
        String eventName = event.getClass().getSimpleName();
        String sessionId = getSessionId(event);

        log.debug("[실시간 같이 보기] {} 감지: sessionId={}", eventName, sessionId);

        try {
            watchingSessionService.leaveWatching(sessionId);
            log.info("[실시간 같이 보기] {} 처리 완료: sessionId={}", eventName, sessionId);

        } catch (WatchingNotFoundException e) {
            log.error("[실시간 같이 보기] {} 처리 중 오류: sessionId={}, error={}",
                    eventName, sessionId, e.getMessage());
        }
    }

    private String getSessionId(Object event) {
        StompHeaderAccessor accessor =
                StompHeaderAccessor.wrap(
                        (event instanceof SessionUnsubscribeEvent)
                                ? ((SessionUnsubscribeEvent) event).getMessage()
                                : ((SessionDisconnectEvent) event).getMessage()
                );

        String sessionId = accessor.getSessionId();
        if (sessionId == null) {
            log.warn("[실시간 같이 보기] {} 에서 sessionId가 null",
                    event.getClass().getSimpleName());
            throw new WatchingNotFoundException();
        }

        return sessionId;
    }
}
