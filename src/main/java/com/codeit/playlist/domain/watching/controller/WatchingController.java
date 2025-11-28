package com.codeit.playlist.domain.watching.controller;

import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.watching.dto.data.SortBy;
import com.codeit.playlist.domain.watching.dto.response.CursorResponseWatchingSessionDto;
import com.codeit.playlist.domain.watching.service.WatchingService;
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
                                                                                @RequestParam String watcherNameLike,
                                                                                @RequestParam(required = false) String cursor,
                                                                                @RequestParam(required = false) UUID idAfter,
                                                                                @RequestParam(required = false) int limit,
                                                                                @RequestParam(defaultValue = "ASCENDING") SortDirection sortDirection,
                                                                                @RequestParam(defaultValue = "createdAt") SortBy sortBy) {
        log.debug("[실시간 같이 보기] 특정 콘텐츠의 시청 세션 목록 조회(커서 페이지네이션) 시작: " +
                        "contentId = {}, watcherNameLike = {}, cursor={}, idAfter={}, limit={}, sortDirection={}, sortBy={}",
                contentId, watcherNameLike, cursor, idAfter, limit, sortDirection, sortBy);

        CursorResponseWatchingSessionDto response =
                watchingService.getWatchingSessions(
                        contentId,
                        watcherNameLike,
                        cursor,
                        idAfter,
                        limit,
                        sortDirection,
                        sortBy
                );

        log.info("[실시간 같이 보기] 특정 콘텐츠의 시청 세션 목록 조회(커서 페이지네이션) 성공: " +
                        "contentId = {}, watcherNameLike = {}, cursor={}, idAfter={}, limit={}, sortDirection={}, sortBy={}",
                contentId, watcherNameLike, cursor, idAfter, limit, sortDirection, sortBy);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }
}
