package com.codeit.playlist.domain.watching.service.basic;

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
import com.codeit.playlist.domain.watching.dto.data.ChangeType;
import com.codeit.playlist.domain.watching.dto.data.RawWatchingSession;
import com.codeit.playlist.domain.watching.dto.data.WatchingSessionDto;
import com.codeit.playlist.domain.watching.dto.response.WatchingSessionChange;
import com.codeit.playlist.domain.watching.event.WatchingSessionPublisher;
import com.codeit.playlist.domain.watching.exception.WatchingSessionUpdateException;
import com.codeit.playlist.domain.watching.repository.RedisWatchingSessionRepository;
import com.codeit.playlist.domain.watching.service.WatchingSessionService;
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
public class BasicWatchingSessionService implements WatchingSessionService {
    private final RedisWatchingSessionRepository redisWatchingSessionRepository;
    private final WatchingSessionPublisher publisher;

    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final ContentRepository contentRepository;
    private final ContentMapper contentMapper;
    private final TagRepository tagRepository;

    @Override
    public void join(UUID contentId, UUID userId) {
        UUID watchingId = UUID.randomUUID();
        log.debug("[실시간 같이 보기] join 시작: watchingId={}, contentId={}, userId={}", watchingId, contentId, userId);

        RawWatchingSession raw = redisWatchingSessionRepository.addWatchingSession(watchingId, contentId, userId);
        if (raw == null) {
            log.error("[실시간 같이 보기] Redis 오류: watchingId={}, contentId={}, userId={}", watchingId, contentId, userId);
            throw new WatchingSessionUpdateException();
        }

        broadcastEvent(raw, ChangeType.JOIN);
    }

    @Override
    public void leave(UUID contentId, UUID userId) {
        log.debug("[실시간 같이 보기] leave 시작: contentId={}, userId={}", contentId, userId);

        RawWatchingSession raw = redisWatchingSessionRepository.removeWatchingSession(userId);
        if (raw == null) {
            log.error("[실시간 같이 보기] Redis 오류: contentId={}, userId={}", contentId, userId);
            throw new WatchingSessionUpdateException();
        }

        broadcastEvent(raw, ChangeType.LEAVE);
    }

    @Override
    public long count(UUID contentId) {
        return redisWatchingSessionRepository.countWatchingSessionByContentId(contentId);
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

    private void broadcastEvent(RawWatchingSession raw, ChangeType type) {
        long watcherCount = count(raw.contentId());
        try {
            WatchingSessionDto watchingSessionDto = createWatchingSessionDto(raw);
            WatchingSessionChange event = new WatchingSessionChange(type, watchingSessionDto, watcherCount);

            publisher.publish(raw.contentId(), event);
            log.info("[실시간 같이 보기] {} 이벤트 생성 완료: watcherCount={}", type, watcherCount);
        } catch (UserNotFoundException | ContentNotFoundException e) {
            log.warn("[실시간 같이 보기] {} 이벤트 생성 실패, count({})만 브로드캐스트:{}", type, watcherCount, e.getMessage());
            WatchingSessionChange event = new WatchingSessionChange(type, null, watcherCount); // TODO: FE 에서 null 받는지 체크
            publisher.publish(raw.contentId(), event);
        }
    }
}
