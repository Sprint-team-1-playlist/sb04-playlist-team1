package com.codeit.playlist.domain.watching.controller;

import com.codeit.playlist.domain.security.PlaylistUserDetails;
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
@MessageMapping("/contents/{contentId}/watch")
public class WatchingSessionController {
    private final WatchingSessionService watchingSessionService;

    @MessageMapping("/join")
    public void joinWatching(@DestinationVariable UUID contentId, Principal principal) {
        UUID userId = ((PlaylistUserDetails) ((Authentication) principal).getPrincipal()).getUserDto().id();
        log.debug("[실시간 같이 보기] 시청자 join 요청 시작: contentId={}, userId={}", contentId, userId);
        watchingSessionService.join(contentId, userId);
        log.info("[실시간 같이 보기] 시청자 join 요청 성공: contentId={}, userId={}", contentId, userId);
    }

    @MessageMapping("/leave")
    public void leaveWatching(@DestinationVariable UUID contentId, Principal principal) {
        UUID userId = ((PlaylistUserDetails) ((Authentication) principal).getPrincipal()).getUserDto().id();
        log.debug("[실시간 같이 보기] 시청자 leave 요청 시작: contentId={}, userId={}", contentId, userId);
        watchingSessionService.leave(contentId, userId);
        log.info("[실시간 같이 보기] 시청자 leave 요청 성공: contentId={}, userId={}", contentId, userId);
    }
}
