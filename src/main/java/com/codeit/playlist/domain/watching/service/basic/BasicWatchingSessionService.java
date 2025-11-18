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
import com.codeit.playlist.domain.watching.dto.data.WatchingSessionDto;
import com.codeit.playlist.domain.watching.dto.response.WatchingSessionChange;
import com.codeit.playlist.domain.watching.event.WatchingSessionPublisher;
import com.codeit.playlist.domain.watching.exception.WatchingSessionUpdateException;
import com.codeit.playlist.domain.watching.repository.RedisWatchingSessionRepository;
import com.codeit.playlist.domain.watching.service.WatchingSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
    public void join(UUID contentId) {
        UUID userId = getCurrentUserId();
        log.debug("[실시간 같이 보기] join 시작: contentId={}, userId={}", contentId, userId);

        boolean added = redisWatchingSessionRepository.addWatcher(contentId, userId);
        if (!added) {
            log.error("[실시간 같이 보기] Redis 저장 실패: contentId={}, userId={}", contentId, userId);
            throw new WatchingSessionUpdateException();
        }
        long watcherCount = redisWatchingSessionRepository.countWatcher(contentId);

        WatchingSessionDto watchingSessionDto = createWatchingSessionDto(contentId);
        WatchingSessionChange event = new WatchingSessionChange(ChangeType.JOIN, watchingSessionDto, watcherCount);

        publisher.publish(contentId, event);
        log.info("[실시간 같이 보기] join 완료: event={}", event);
    }

    @Override
    public void leave(UUID contentId) {
        UUID userId = getCurrentUserId();
        log.debug("[실시간 같이 보기] leave 시작: contentId={}, userId={}", contentId, userId);

        boolean isAdded = redisWatchingSessionRepository.removeWatcher(contentId, userId);
        if (!isAdded) {
            log.error("[실시간 같이 보기] Redis 삭제 실패: contentId={}, userId={}", contentId, userId);
            throw new WatchingSessionUpdateException();
        }
        long watcherCount = redisWatchingSessionRepository.countWatcher(contentId);

        try {
            WatchingSessionDto watchingSessionDto = createWatchingSessionDto(contentId);
            WatchingSessionChange event = new WatchingSessionChange(ChangeType.LEAVE, watchingSessionDto, watcherCount);

            publisher.publish(contentId, event);

        } catch (UserNotFoundException | ContentNotFoundException e) {
            log.warn("[실시간 같이 보기] leave 이벤트 생성 실패, count({})만 브로드캐스트:{}", watcherCount, e.getMessage());
            WatchingSessionChange event = new WatchingSessionChange(ChangeType.LEAVE, null, watcherCount); // TODO: FE 에서 null 받는지 체크
            publisher.publish(contentId, event);
        }

        log.info("[실시간 같이 보기] leave 완료: watcherCount={}", watcherCount);
    }

    private WatchingSessionDto createWatchingSessionDto(UUID contentId) {
        User user = userRepository.findById(getCurrentUserId())
                .orElseThrow(() -> UserNotFoundException.withId(getCurrentUserId()));
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> ContentNotFoundException.withId(contentId));
        List<Tag> tags = tagRepository.findByContentId(contentId);

        return new WatchingSessionDto(
                UUID.randomUUID(),
                LocalDateTime.now(),
                userMapper.toDto(user),
                contentMapper.toDto(content, tags)
        );
    }

    private UUID getCurrentUserId() {
        return UUID.fromString("5a160ff2-5420-4329-b69b-65427396ebbe"); // TODO: security 구현되면 authentication 가져와서 대체하기
    }
}
