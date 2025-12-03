package com.codeit.playlist.domain.watching.service.basic;

import com.codeit.playlist.domain.content.entity.Content;
import com.codeit.playlist.domain.content.entity.Tag;
import com.codeit.playlist.domain.content.exception.ContentNotFoundException;
import com.codeit.playlist.domain.content.mapper.ContentMapper;
import com.codeit.playlist.domain.content.repository.ContentRepository;
import com.codeit.playlist.domain.content.repository.TagRepository;
import com.codeit.playlist.domain.user.dto.data.UserSummary;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.exception.UserNotFoundException;
import com.codeit.playlist.domain.user.mapper.UserMapper;
import com.codeit.playlist.domain.user.repository.UserRepository;
import com.codeit.playlist.domain.watching.dto.data.ChangeType;
import com.codeit.playlist.domain.watching.dto.data.RawContentChat;
import com.codeit.playlist.domain.watching.dto.data.RawWatchingSession;
import com.codeit.playlist.domain.watching.dto.data.WatchingSessionDto;
import com.codeit.playlist.domain.watching.dto.request.ContentChatSendRequest;
import com.codeit.playlist.domain.watching.dto.response.ContentChatDto;
import com.codeit.playlist.domain.watching.dto.response.WatchingSessionChange;
import com.codeit.playlist.domain.watching.event.WatchingSessionPublisher;
import com.codeit.playlist.domain.watching.exception.WatchingSessionMismatch;
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

        broadcastWatchingEvent(raw, ChangeType.JOIN);
    }

    @Override
    public void leave(UUID contentId, UUID userId) {
        log.debug("[실시간 같이 보기] leave 시작: contentId={}, userId={}", contentId, userId);

        RawWatchingSession raw = redisWatchingSessionRepository.removeWatchingSession(userId);
        if (raw == null) {
            log.error("[실시간 같이 보기] Redis 오류: contentId={}, userId={}", contentId, userId);
            throw new WatchingSessionUpdateException();
        }
        if (!contentId.equals(raw.contentId())) {
            log.error("[실시간 같이 보기] 세션 정보 불일치");
            throw WatchingSessionMismatch.withWatchingIdAndContentId(raw.watchingId(), raw.contentId());
        }

        broadcastWatchingEvent(raw, ChangeType.LEAVE);
    }

    @Override
    public long count(UUID contentId) {
        return redisWatchingSessionRepository.countWatchingSessionByContentId(contentId);
    }

    @Override
    public void sendChat(UUID contentId, UUID userId, ContentChatSendRequest request) {
        log.debug("[실시간 같이 보기] 채팅 수신 비즈니스 로직 시작:  contentId={}, userId={}, request={}", contentId, userId, request);

        String chatData = userId + ":" + request.content();
        RawContentChat raw = redisWatchingSessionRepository.addChat(contentId, chatData);
        if (raw == null) {
            log.error("[실시간 같이 보기] 채팅 관련 Redis 오류: contentId={}, userId={}, request={}", contentId, userId, request);
            throw WatchingSessionUpdateException.withContentIdUserId(contentId, userId);
        }

        broadcastChatEvent(contentId, createContentChatDto(raw));

        log.info("[실시간 같이 보기] 채팅 수신 비즈니스 로직 성공");
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

    private ContentChatDto createContentChatDto(RawContentChat raw) {
        User user = userRepository.findById(raw.userId())
                .orElseThrow(() -> UserNotFoundException.withId(raw.userId()));
        UserSummary sender = new UserSummary(
                user.getId(),
                user.getName(),
                user.getProfileImageUrl()
        );

        return new ContentChatDto(
                sender,
                raw.content()
        );
    }

    private void broadcastWatchingEvent(RawWatchingSession raw, ChangeType type) {
        long watcherCount = count(raw.contentId());
        try {
            WatchingSessionDto watchingSessionDto = createWatchingSessionDto(raw);
            WatchingSessionChange event = new WatchingSessionChange(type, watchingSessionDto, watcherCount);

            publisher.publishWatching(raw.contentId(), event);
            log.info("[실시간 같이 보기] {} 이벤트 생성 완료: watcherCount={}", type, watcherCount);
        } catch (UserNotFoundException | ContentNotFoundException e) {
            log.warn("[실시간 같이 보기] {} 이벤트 생성 실패, count({})만 브로드캐스트:{}", type, watcherCount, e.getMessage());
            WatchingSessionChange event = new WatchingSessionChange(type, null, watcherCount); // TODO: FE 에서 null 받는지 체크
            publisher.publishWatching(raw.contentId(), event);
        }
    }

    private void broadcastChatEvent(UUID contentId, ContentChatDto contentChatDto) {
        log.debug("[실시간 같이 보기] 채팅 이벤트 발송 시작: {}", contentChatDto);
        try {
            publisher.publishChat(contentId, contentChatDto);
            log.info("[실시간 같이 보기] 채팅 이벤트 발송 성공: {}", contentChatDto);
        } catch (Exception e) {
            log.error("[실시간 같이 보기] 채팅 이벤트 발송 실패: contentChatDto={}, errorMsg={}", contentChatDto, e.getMessage());
        }
    }
}
