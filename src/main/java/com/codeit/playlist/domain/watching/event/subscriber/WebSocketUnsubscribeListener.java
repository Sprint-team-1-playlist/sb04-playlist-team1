package com.codeit.playlist.domain.watching.event.subscriber;

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
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        String sessionId = accessor.getSessionId();

        log.debug("[실시간 같이 보기] UNSUBSCRIBE 감지: sessionId={}", sessionId);
        watchingSessionService.leaveWatching(sessionId);
        log.info("[실시간 같이 보기] UNSUBSCRIBE 시청 세션 처리 완료: sessionId={}", sessionId);
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        String sessionId = accessor.getSessionId();

        watchingSessionService.leaveWatching(sessionId);
    }
}
