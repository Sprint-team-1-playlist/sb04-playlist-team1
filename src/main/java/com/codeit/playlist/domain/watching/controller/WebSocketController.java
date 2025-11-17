package com.codeit.playlist.domain.watching.controller;

import com.codeit.playlist.domain.watching.service.WatchingSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
@MessageMapping("/contents/{contentId}/watch")
public class WebSocketController {
    private final WatchingSessionService watchingSessionService;

    @MessageMapping("/join")
    public void joinWatching(@DestinationVariable UUID contentId) {
        log.debug("[실시간 같이 보기] 시청자 join 요청 시작: contentId={}", contentId);
        watchingSessionService.join(contentId);
        log.info("[실시간 같이 보기] 시청자 join 요청 성공: contentId={}", contentId);
    }

    @MessageMapping("/leave")
    public void leaveWatching(@DestinationVariable UUID contentId) {
        log.debug("[실시간 같이 보기] 시청자 leave 요청 시작: contentId={}", contentId);
        watchingSessionService.leave(contentId);
        log.info("[실시간 같이 보기] 시청자 leave 요청 성공: contentId={}", contentId);
    }
}
