package com.codeit.playlist.watching.service.basic;

import com.codeit.playlist.domain.content.mapper.ContentMapper;
import com.codeit.playlist.domain.content.repository.ContentRepository;
import com.codeit.playlist.domain.content.repository.TagRepository;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.exception.UserNotFoundException;
import com.codeit.playlist.domain.user.mapper.UserMapper;
import com.codeit.playlist.domain.user.repository.UserRepository;
import com.codeit.playlist.domain.watching.dto.data.RawContentChat;
import com.codeit.playlist.domain.watching.dto.data.RawWatchingSession;
import com.codeit.playlist.domain.watching.dto.request.ContentChatSendRequest;
import com.codeit.playlist.domain.watching.dto.response.ContentChatDto;
import com.codeit.playlist.domain.watching.dto.response.WatchingSessionChange;
import com.codeit.playlist.domain.watching.event.publisher.WatchingSessionPublisher;
import com.codeit.playlist.domain.watching.exception.WatchingSessionUpdateException;
import com.codeit.playlist.domain.watching.repository.RedisWatchingSessionRepository;
import com.codeit.playlist.domain.watching.service.basic.BasicWatchingSessionService;
import com.codeit.playlist.watching.fixture.WatchingSessionFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BasicWatchingSessionServiceTest {
    @InjectMocks
    private BasicWatchingSessionService watchingSessionService;

    @Mock
    private RedisWatchingSessionRepository redisWatchingSessionRepository;
    @Mock
    private WatchingSessionPublisher publisher;
    @Mock
    private UserMapper userMapper;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ContentRepository contentRepository;
    @Mock
    private ContentMapper contentMapper;
    @Mock
    private TagRepository tagRepository;

    private final UUID contentId = WatchingSessionFixtures.FIXED_ID;
    private final UUID userId = WatchingSessionFixtures.FIXED_ID;

    @Test
    @DisplayName("watching() 호출 시 Redis 저장 후 Watching 이벤트 publish")
    void watchingShouldPublishEvent() {
        // given
        RawWatchingSession raw = WatchingSessionFixtures.rawWatchingSession();

        when(redisWatchingSessionRepository.addWatchingSession(any(), eq(contentId), eq(userId)))
                .thenReturn(raw);
        when(userRepository.findById(userId))
                .thenReturn(java.util.Optional.of(WatchingSessionFixtures.user()));
        when(contentRepository.findById(contentId))
                .thenReturn(java.util.Optional.of(WatchingSessionFixtures.content()));
        when(tagRepository.findByContentId(contentId))
                .thenReturn(WatchingSessionFixtures.tagList());
        when(redisWatchingSessionRepository.countWatchingSessionByContentId(contentId))
                .thenReturn(3L);

        // when
        watchingSessionService.watching(contentId, userId);

        // then
        verify(redisWatchingSessionRepository, times(1))
                .addWatchingSession(any(), eq(contentId), eq(userId));
        verify(publisher, times(1))
                .publishWatching(eq(contentId), any(WatchingSessionChange.class));
    }

    @Test
    @DisplayName("watching() Redis 가 null 반환 시 예외 발생")
    void watchingShouldThrowWhenRedisFails() {
        // given
        when(redisWatchingSessionRepository.addWatchingSession(any(), eq(contentId), eq(userId)))
                .thenReturn(null);

        // when & then
        assertThatThrownBy(() -> watchingSessionService.watching(contentId, userId))
                .isInstanceOf(WatchingSessionUpdateException.class);
    }

    @Test
    @DisplayName("sendChat() 정상 처리 시 publishChat 호출")
    void sendChatShouldPublish() {
        // given
        ContentChatSendRequest request = new ContentChatSendRequest("content");

        RawContentChat raw = WatchingSessionFixtures.rawContentChat();

        when(redisWatchingSessionRepository.addChat(contentId, userId, "content"))
                .thenReturn(raw);

        User user = WatchingSessionFixtures.user();
        when(user.getId()).thenReturn(userId);
        when(user.getName()).thenReturn("name");
        when(user.getProfileImageUrl()).thenReturn("profileImageUrl");
        when(userRepository.findById(userId))
                .thenReturn(java.util.Optional.of(user));

        // when
        watchingSessionService.sendChat(contentId, userId, request);

        // then
        verify(publisher, times(1))
                .publishChat(eq(contentId), any(ContentChatDto.class));
    }

    @Test
    @DisplayName("sendChat() Redis 실패 시 예외 발생")
    void sendChatShouldThrowWhenRedisFails() {
        ContentChatSendRequest request = new ContentChatSendRequest("content");

        when(redisWatchingSessionRepository.addChat(contentId, userId, "content"))
                .thenReturn(null);

        assertThatThrownBy(() -> watchingSessionService.sendChat(contentId, userId, request))
                .isInstanceOf(WatchingSessionUpdateException.class);
    }

    @Test
    @DisplayName("watching 이벤트 생성 중 UserNotFoundException 발생 시 fallback 이벤트 발송")
    void watchingEventFallbackWhenUserMissing() {
        // given
        RawWatchingSession raw = WatchingSessionFixtures.rawWatchingSession();

        when(redisWatchingSessionRepository.addWatchingSession(any(), eq(contentId), eq(userId)))
                .thenReturn(raw);
        when(redisWatchingSessionRepository.countWatchingSessionByContentId(contentId)).thenReturn(3L);

        when(userRepository.findById(userId))
                .thenThrow(UserNotFoundException.withId(userId));

        // when
        watchingSessionService.watching(contentId, userId);

        // then
        ArgumentCaptor<WatchingSessionChange> captor = ArgumentCaptor.forClass(WatchingSessionChange.class);

        verify(publisher).publishWatching(eq(contentId), captor.capture());

        WatchingSessionChange event = captor.getValue();

        // watchingSession == null 이면 fallback 성공
        assert event.watchingSession() == null;
        assert event.watcherCount() == 3;
    }

}