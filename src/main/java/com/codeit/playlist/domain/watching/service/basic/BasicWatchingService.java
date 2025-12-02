package com.codeit.playlist.domain.watching.service.basic;

import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.content.entity.Content;
import com.codeit.playlist.domain.content.entity.Tag;
import com.codeit.playlist.domain.content.exception.ContentNotFoundException;
import com.codeit.playlist.domain.content.mapper.ContentMapper;
import com.codeit.playlist.domain.content.repository.ContentRepository;
import com.codeit.playlist.domain.content.repository.TagRepository;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.exception.UserNotFoundException;
import com.codeit.playlist.domain.user.mapper.UserMapper;
import com.codeit.playlist.domain.user.repository.UserRepository;
import com.codeit.playlist.domain.watching.dto.data.RawWatchingSession;
import com.codeit.playlist.domain.watching.dto.data.RawWatchingSessionPage;
import com.codeit.playlist.domain.watching.dto.data.SortBy;
import com.codeit.playlist.domain.watching.dto.data.WatchingSessionDto;
import com.codeit.playlist.domain.watching.dto.response.CursorResponseWatchingSessionDto;
import com.codeit.playlist.domain.watching.repository.RedisWatchingSessionRepository;
import com.codeit.playlist.domain.watching.service.WatchingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BasicWatchingService implements WatchingService {

    private final RedisWatchingSessionRepository redisWatchingSessionRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final ContentRepository contentRepository;
    private final ContentMapper contentMapper;
    private final TagRepository tagRepository;

    @Override
    public CursorResponseWatchingSessionDto getWatchingSessionsByContent(UUID contentId,
                                                                         String watcherNameLike,
                                                                         String cursor,
                                                                         UUID idAfter,
                                                                         int limit,
                                                                         SortDirection sortDirection,
                                                                         SortBy sortBy) {
        log.debug("[실시간 같이 보기] 실시간 시청자 조회 시작: " +
                        "contentId = {}, watcherNameLike = {}, cursor={}, idAfter={}, limit={}, sortDirection={}, sortBy={}",
                contentId, watcherNameLike, cursor, idAfter, limit, sortDirection, sortBy);

        if (limit <= 0 || limit > 50) {
            log.error("[실시간 같이 보기] 유효하지 않은 파라미터, 기본값으로 보정");
            limit = 10;
        }

        RawWatchingSessionPage page = redisWatchingSessionRepository.getWatchingSessionsByContentId(
                contentId,
                cursor,
                limit,
                sortDirection);

        List<WatchingSessionDto> dtos = page.raws().stream()
                .map(this::createWatchingSessionDto)
                .toList();

        long totalCount = redisWatchingSessionRepository.countWatchingSessionByContentId(contentId);
        String nextCursor = null;
        UUID nextIdAfter = null;
        if (page.hasNext()) {
            RawWatchingSession last = page.raws().get(page.raws().size() - 1);
            nextCursor = String.valueOf(last.createdAtEpoch());
            nextIdAfter = last.watchingId();
        }

        log.info("[실시간 같이 보기] 실시간 시청자 조회 성공: " +
                        "contentId = {}, watcherNameLike = {}, cursor={}, idAfter={}, limit={}, sortDirection={}, sortBy={}",
                contentId, watcherNameLike, cursor, idAfter, limit, sortDirection, sortBy);

        return new CursorResponseWatchingSessionDto(
                dtos,
                nextCursor,
                nextIdAfter,
                page.hasNext(),
                totalCount,
                sortBy,
                sortDirection
        );
    }

    @Override
    public WatchingSessionDto getWatchingSessionByUser(UUID userId) {
        log.debug("[실시간 같이 보기] 특정 사용자의 시청 세션 조회(nullable) 비즈니스 로직 시작: watcherId={}", userId);

        RawWatchingSession raw = redisWatchingSessionRepository.getWatchingSessionByUser(userId);
        if (raw == null) {
            return null;
        }
        log.debug("[실시간 같이 보기] 특정 사용자의 시청 세션 조회(nullable) 비즈니스 로직 성공: raw={}", raw);
        return createWatchingSessionDto(raw);
    }

    private WatchingSessionDto createWatchingSessionDto(RawWatchingSession raw) {
        User user = userRepository.findById(raw.userId())
                .orElseThrow(() -> UserNotFoundException.withId(raw.userId()));
        Content content = contentRepository.findById(raw.contentId())
                .orElseThrow(() -> ContentNotFoundException.withId(raw.contentId()));
        List<Tag> tags = tagRepository.findByContentId(raw.contentId());

        LocalDateTime createdAt = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(raw.createdAtEpoch()),
                ZoneId.systemDefault()
        );

        return new WatchingSessionDto(
                raw.watchingId(),
                createdAt,
                userMapper.toDto(user),
                contentMapper.toDto(content, tags)
        );
    }
}
