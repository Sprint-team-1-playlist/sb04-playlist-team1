package com.codeit.playlist.domain.watching.controller;

import com.codeit.playlist.domain.watching.dto.request.WatchingSessionRequest;
import com.codeit.playlist.domain.watching.dto.response.CursorResponseWatchingSessionDto;
import com.codeit.playlist.domain.watching.service.WatchingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class WatchingController {
    private final WatchingService watchingService;

    @GetMapping("/contents/{contentId}/watching-sessions")
    public ResponseEntity<CursorResponseWatchingSessionDto> getWatchingSessions(@PathVariable("contentId") UUID contentId,
                                                                                @Valid @ModelAttribute WatchingSessionRequest request) {
        log.debug("[실시간 같이 보기] 특정 콘텐츠의 시청 세션 목록 조회(커서 페이지네이션) 시작: contentId = {}, request = {}", contentId, request);
        CursorResponseWatchingSessionDto response = watchingService.getWatchingSessions(contentId, request);
        log.info("[실시간 같이 보기] 특정 콘텐츠의 시청 세션 목록 조회(커서 페이지네이션) 완료: contentId = {}, response = {}", contentId, response);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }
}
