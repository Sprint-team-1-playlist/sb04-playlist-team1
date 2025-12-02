package com.codeit.playlist.domain.watching.controller;

import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.watching.dto.data.WatchingSessionDto;
import com.codeit.playlist.domain.watching.dto.data.WatchingSortBy;
import com.codeit.playlist.domain.watching.dto.response.CursorResponseWatchingSessionDto;
import com.codeit.playlist.domain.watching.service.WatchingService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
    public ResponseEntity<CursorResponseWatchingSessionDto> getWatchingSessionsByContent(@PathVariable("contentId") UUID contentId,
                                                                                         @RequestParam(required = false) String watcherNameLike,
                                                                                         @RequestParam(required = false) String cursor,
                                                                                         @RequestParam(required = false) UUID idAfter,
                                                                                         @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit,
                                                                                         @RequestParam(defaultValue = "ASCENDING") SortDirection sortDirection,
                                                                                         @RequestParam(defaultValue = "createdAt") WatchingSortBy sortBy) {
        log.debug("[실시간 같이 보기] 특정 콘텐츠의 시청 세션 목록 조회(커서 페이지네이션) 시작: " +
                        "contentId = {}, watcherNameLike = {}, cursor={}, idAfter={}, limit={}, sortDirection={}, sortBy={}",
                contentId, watcherNameLike, cursor, idAfter, limit, sortDirection, sortBy);

        CursorResponseWatchingSessionDto response =
                watchingService.getWatchingSessionsByContent(
                        contentId,
                        watcherNameLike,
                        cursor,
                        idAfter,
                        limit,
                        sortDirection,
                        sortBy
                );

        log.info("[실시간 같이 보기] 특정 콘텐츠의 시청 세션 목록 조회(커서 페이지네이션) 성공: response={}", response);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @GetMapping("/users/{watcherId}/watching-sessions")
    public ResponseEntity<WatchingSessionDto> getWatchingSessionByUser(@PathVariable("watcherId") UUID watcherId) {
        log.debug("[실시간 같이 보기] 특정 사용자의 시청 세션 조회(nullable) 시작: watcherId={}", watcherId);

        WatchingSessionDto response = watchingService.getWatchingSessionByUser(watcherId);

        log.info("[실시간 같이 보기] 특정 사용자의 시청 세션 조회(nullable) 완료: response={}", response);

        if (response == null) {
            return ResponseEntity
                    .status(HttpStatus.NO_CONTENT)
                    .build();
        }
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }
}
