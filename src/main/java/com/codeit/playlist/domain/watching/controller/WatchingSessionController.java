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

    @MessageMapping("/chat")
    public void sendChat(@DestinationVariable UUID contentId,
                         Principal principal,
                         ContentChatSendRequest request) {
        UUID userId = getUserId(principal);
        log.debug("[실시간 같이 보기] chat 수신 시작: contentId={}, userId={}", contentId, userId);
        watchingSessionService.sendChat(contentId, userId, request);
        log.info("[실시간 같이 보기] chat 수신 완료:  contentId={}, userId={}", contentId, userId);
    }

    private UUID getUserId(Principal principal) {
        return ((PlaylistUserDetails) ((Authentication) principal).getPrincipal()).getUserDto().id();
    }
}
