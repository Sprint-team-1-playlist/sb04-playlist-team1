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
import com.codeit.playlist.domain.watching.exception.WatchingSessionUpdateException;
import com.codeit.playlist.domain.watching.repository.RedisWatchingSessionRepository;
import com.codeit.playlist.domain.watching.service.WatchingSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BasicWatchingSessionService implements WatchingSessionService {
    private final RedisWatchingSessionRepository redisWatchingSessionRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final ContentRepository contentRepository;
    private final ContentMapper contentMapper;
    private final TagRepository tagRepository;

    public void join(UUID contentId) {
        UUID userId = getCurrentUserId();
        log.debug("[실시간 같이 보기] join 시작: contentId={}, userId={}", contentId, userId);

        boolean isAdded = redisWatchingSessionRepository.addWatcher(contentId, userId);
        if (!isAdded) {
            log.error("[실시간 같이 보기] Redis 저장 실패: contentId={}, userId={}", contentId, userId);
            throw new WatchingSessionUpdateException();
        }
        long watcherCount = redisWatchingSessionRepository.countWatcher(contentId);

        WatchingSessionDto watchingSessionDto = createWatchingSessionDto(contentId);
        WatchingSessionChange event = createEvent(ChangeType.JOIN, watchingSessionDto, watcherCount);

        sendToDestination(contentId, event);
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

        WatchingSessionDto watchingSessionDto = createWatchingSessionDto(contentId);
        WatchingSessionChange event = createEvent(ChangeType.LEAVE, watchingSessionDto, watcherCount);

        sendToDestination(contentId, event);
        log.info("[실시간 같이 보기] leave 완료: watcherCount={}", watcherCount);
    }

    private void sendToDestination(UUID contentId, WatchingSessionChange event) {
        messagingTemplate.convertAndSend(
                "/sub/contents/" + contentId + "/watch",
                event
        );
    }

    private WatchingSessionChange createEvent(ChangeType type, WatchingSessionDto watchingSessionDto, long watcherCount) {
        return new WatchingSessionChange(
                type,
                watchingSessionDto,
                watcherCount
        );
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
        return UUID.randomUUID(); // TODO: security 구현되면 authentication 가져와서 대체하기
    }
}
