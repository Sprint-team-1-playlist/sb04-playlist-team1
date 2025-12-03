package com.codeit.playlist.watching.service.basic;

import com.codeit.playlist.domain.content.dto.data.ContentDto;
import com.codeit.playlist.domain.content.entity.Content;
import com.codeit.playlist.domain.content.entity.Tag;
import com.codeit.playlist.domain.content.mapper.ContentMapper;
import com.codeit.playlist.domain.content.repository.ContentRepository;
import com.codeit.playlist.domain.content.repository.TagRepository;
import com.codeit.playlist.domain.user.dto.data.UserDto;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.mapper.UserMapper;
import com.codeit.playlist.domain.user.repository.UserRepository;
import com.codeit.playlist.domain.watching.dto.data.ChangeType;
import com.codeit.playlist.domain.watching.dto.data.RawContentChat;
import com.codeit.playlist.domain.watching.dto.data.RawWatchingSession;
import com.codeit.playlist.domain.watching.dto.request.ContentChatSendRequest;
import com.codeit.playlist.domain.watching.dto.response.ContentChatDto;
import com.codeit.playlist.domain.watching.dto.response.WatchingSessionChange;
import com.codeit.playlist.domain.watching.event.WatchingSessionPublisher;
import com.codeit.playlist.domain.watching.repository.RedisWatchingSessionRepository;
import com.codeit.playlist.domain.watching.service.basic.BasicWatchingSessionService;
import com.codeit.playlist.watching.fixture.WatchingSessionFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Captor
    private ArgumentCaptor<WatchingSessionChange> eventCaptor;
    @Captor
    private ArgumentCaptor<ContentChatDto> contentChatCaptor;

    private final UUID contentId = WatchingSessionFixtures.FIXED_ID;
    private final UUID userId = WatchingSessionFixtures.FIXED_ID;

    @Test
    @DisplayName("join() 호출 시 addWatchingSession, countWatchingSession, publish 순서대로 호출")
    void joinShouldPerformAllSteps() {
        // given
        User user = WatchingSessionFixtures.user();
        when(userRepository.findById(any())).thenReturn(Optional.of(user));

        UserDto userDto = WatchingSessionFixtures.userDto();
        when(userMapper.toDto(any())).thenReturn(userDto);

        Content content = WatchingSessionFixtures.content();
        ContentDto contentDto = WatchingSessionFixtures.contentDto();
        when(contentMapper.toDto(any(), any())).thenReturn(contentDto);
        List<Tag> tags = WatchingSessionFixtures.tagList();

        when(contentRepository.findById(any())).thenReturn(Optional.of(content));
        when(tagRepository.findByContentId(any())).thenReturn(tags);
        when(redisWatchingSessionRepository.countWatchingSessionByContentId(any()))
                .thenReturn(3L);

        RawWatchingSession raw = WatchingSessionFixtures.rawWatchingSession();
        when(redisWatchingSessionRepository.addWatchingSession(any(), any(), any()))
                .thenReturn(raw);

        // when
        watchingSessionService.join(contentId, userId);

        // then
        verify(redisWatchingSessionRepository).addWatchingSession(any(), eq(contentId), eq(userId));
        verify(redisWatchingSessionRepository).countWatchingSessionByContentId(contentId);
        verify(userRepository).findById(userId);
        verify(contentRepository).findById(contentId);
        verify(tagRepository).findByContentId(contentId);
        verify(publisher).publishWatching(eq(contentId), eventCaptor.capture());

        WatchingSessionChange event = eventCaptor.getValue();
        assertThat(event.type()).isEqualTo(ChangeType.JOIN);
        assertThat(event.watcherCount()).isEqualTo(3L);
        assertThat(event.watchingSession()).isNotNull();
    }

    @Test
    @DisplayName("leave() 호출 시 removeWatchingSession, countWatchingSession, publish 순서대로 호출")
    void leaveShouldPerformAllSteps() {
        // given
        User user = WatchingSessionFixtures.user();
        when(userRepository.findById(any())).thenReturn(Optional.of(user));

        UserDto userDto = WatchingSessionFixtures.userDto();
        when(userMapper.toDto(any())).thenReturn(userDto);

        Content content = WatchingSessionFixtures.content();
        ContentDto contentDto = WatchingSessionFixtures.contentDto();
        when(contentMapper.toDto(any(), any())).thenReturn(contentDto);
        List<Tag> tags = WatchingSessionFixtures.tagList();

        when(contentRepository.findById(any())).thenReturn(Optional.of(content));
        when(tagRepository.findByContentId(any())).thenReturn(tags);
        when(redisWatchingSessionRepository.countWatchingSessionByContentId(any()))
                .thenReturn(3L);

        RawWatchingSession raw = WatchingSessionFixtures.rawWatchingSession();
        when(redisWatchingSessionRepository.removeWatchingSession(eq(userId)))
                .thenReturn(raw);

        // when
        watchingSessionService.leave(contentId, userId);

        // given
        verify(redisWatchingSessionRepository).removeWatchingSession(userId);
        verify(redisWatchingSessionRepository).countWatchingSessionByContentId(contentId);
        verify(userRepository).findById(userId);
        verify(contentRepository).findById(contentId);
        verify(tagRepository).findByContentId(contentId);
        verify(publisher).publishWatching(eq(contentId), eventCaptor.capture());

        WatchingSessionChange event = eventCaptor.getValue();
        assertThat(event.type()).isEqualTo(ChangeType.LEAVE);
        assertThat(event.watcherCount()).isEqualTo(3L);
        assertThat(event.watchingSession()).isNotNull();
    }

    @Test
    @DisplayName("sendChat 호출 시 Redis 저장 후 Chat publish 호출")
    void sendChatShouldPublishChatEvent() {
        RawContentChat raw = new RawContentChat(userId, "hello");
        when(redisWatchingSessionRepository.addChat(eq(contentId), eq(userId), anyString()))
                .thenReturn(raw);

        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        when(user.getName()).thenReturn("name");
        when(user.getProfileImageUrl()).thenReturn("profileImageUrl");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // when
        ContentChatSendRequest request = new ContentChatSendRequest("hello");
        watchingSessionService.sendChat(contentId, userId, request);

        // then
        verify(redisWatchingSessionRepository).addChat(eq(contentId), eq(userId), anyString());
        verify(publisher).publishChat(eq(contentId), contentChatCaptor.capture());

        ContentChatDto publishedDto = contentChatCaptor.getValue();
        assertThat(publishedDto.sender().userId()).isEqualTo(userId);
        assertThat(publishedDto.sender().name()).isEqualTo("name");
        assertThat(publishedDto.sender().profileImageUrl()).isEqualTo("profileImageUrl");
        assertThat(publishedDto.content()).isEqualTo("hello");
    }
}