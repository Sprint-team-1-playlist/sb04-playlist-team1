package com.codeit.playlist.domain.watching.event.subscriber;

import com.codeit.playlist.domain.security.PlaylistUserDetails;
import com.codeit.playlist.domain.watching.service.WatchingSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.security.Principal;
import java.util.UUID;


@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketSubscribeListener implements ApplicationListener<SessionSubscribeEvent> {
    private final WatchingSessionService watchingSessionService;

    @Override
    public void onApplicationEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        if (accessor.getCommand() != StompCommand.SUBSCRIBE) {
            return;
        }

        String destination = accessor.getDestination();
        String sessionId = accessor.getSessionId();
        Principal principal = accessor.getUser();

        if (destination == null || principal == null) {
            return;
        }

        // /sub/contents/{contentId}/watch 패턴만 처리
        if (destination.matches("/sub/contents/.*/watch")) {
            try {
                String[] parts = destination.split("/");
                UUID contentId = UUID.fromString(parts[3]); // {contentId} 추출
                UUID userId = getUserId(principal);

                log.debug("[실시간 같이 보기] SUBSCRIBE 감지: sessionId={}, contentId={}, userId={}", sessionId, contentId, userId);
                watchingSessionService.joinWatching(sessionId, contentId, userId);
                log.info("[실시간 같이 보기] SUBSCRIBE 시청 세션 처리 완료: sessionId={}", sessionId);
            } catch (Exception e) {
                log.error("[실시간 같이 보기] SUBSCRIBE 처리 실패: destination={}, error={}", destination, e.getMessage(), e);
            }
        }
    }

    private UUID getUserId(Principal principal) {
        return ((PlaylistUserDetails) ((Authentication) principal).getPrincipal()).getUserDto().id();
    }
}
