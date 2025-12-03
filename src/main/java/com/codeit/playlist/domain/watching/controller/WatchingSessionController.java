package com.codeit.playlist.domain.watching.controller;

import com.codeit.playlist.domain.security.PlaylistUserDetails;
import com.codeit.playlist.domain.watching.dto.request.ContentChatSendRequest;
import com.codeit.playlist.domain.watching.service.WatchingSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
@MessageMapping("/contents/{contentId}")
public class WatchingSessionController {
    private final WatchingSessionService watchingSessionService;

    @MessageMapping("/watch/join")
    public void joinWatching(@DestinationVariable UUID contentId, Principal principal) {
        UUID userId = getUserId(principal);
        log.debug("[실시간 같이 보기] 시청자 join 요청 시작: contentId={}, userId={}", contentId, userId);
        watchingSessionService.join(contentId, userId);
        log.info("[실시간 같이 보기] 시청자 join 요청 성공: contentId={}, userId={}", contentId, userId);
    }

    @MessageMapping("/watch/leave")
    public void leaveWatching(@DestinationVariable UUID contentId, Principal principal) {
        UUID userId = getUserId(principal);
        log.debug("[실시간 같이 보기] 시청자 leave 요청 시작: contentId={}, userId={}", contentId, userId);
        watchingSessionService.leave(contentId, userId);
        log.info("[실시간 같이 보기] 시청자 leave 요청 성공: contentId={}, userId={}", contentId, userId);
    }

    @MessageMapping("/chat")
    public void sendChat(@DestinationVariable UUID contentId,
                         Principal principal,
                         ContentChatSendRequest request) {
        UUID userId = getUserId(principal);
        log.debug("[실시간 같이 보기] chat 수신 시작: contentId={}, userId={}, request={}", contentId, userId, request);
        watchingSessionService.sendChat(contentId, userId, request);
        log.info("[실시간 같이 보기] chat 수신 완료:  contentId={}, userId={}, request={}", contentId, userId, request);
    }

    private UUID getUserId(Principal principal) {
        return ((PlaylistUserDetails) ((Authentication) principal).getPrincipal()).getUserDto().id();
    }
}
