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
import com.codeit.playlist.domain.watching.dto.data.WatchingSessionDto;
import com.codeit.playlist.domain.watching.dto.response.WatchingSessionChange;
import com.codeit.playlist.domain.watching.event.WatchingSessionPublisher;
import com.codeit.playlist.domain.watching.repository.RedisWatchingSessionRepository;
import com.codeit.playlist.domain.watching.service.basic.BasicWatchingSessionService;
import com.codeit.playlist.watching.fixture.WatchingSessionFixtures;
import org.junit.jupiter.api.BeforeEach;
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

    private final UUID contentId = WatchingSessionFixtures.FIXED_ID;
    private final WatchingSessionDto watchingSessionDto = WatchingSessionFixtures.watchingSessionDto();

    @BeforeEach
    void setUp() {
        User user = WatchingSessionFixtures.user();
        UserDto userDto = WatchingSessionFixtures.userDto();
        Content content = WatchingSessionFixtures.content();
        ContentDto contentDto = WatchingSessionFixtures.contentDto();
        List<Tag> tags = WatchingSessionFixtures.tagList();

        when(userRepository.findById(any())).thenReturn(Optional.of(user));
        when(contentRepository.findById(any())).thenReturn(Optional.of(content));
        when(tagRepository.findByContentId(any())).thenReturn(tags);
        when(userMapper.toDto(any())).thenReturn(userDto);
        when(contentMapper.toDto(any(), any())).thenReturn(contentDto);
    }

    @Test
    @DisplayName("join 호출 시 Redis add, count, repository 조회, 메시지 전송 모두 수행")
    void joinShouldPerformAllSteps() {
        // given
        when(redisWatchingSessionRepository.addWatcher(any(), any())).thenReturn(true);
        when(redisWatchingSessionRepository.countWatcher(any())).thenReturn(3L);

        // when
        watchingSessionService.join(contentId);

        // then
        verify(redisWatchingSessionRepository, times(1))
                .addWatcher(eq(contentId), any());
        verify(redisWatchingSessionRepository, times(1))
                .countWatcher(eq(contentId));
        verify(userRepository, times(1))
                .findById(any());
        verify(contentRepository, times(1))
                .findById(any());
        verify(tagRepository, times(1))
                .findByContentId(any());
        verify(publisher, times(1))
                .publish(eq(contentId), eventCaptor.capture());

        WatchingSessionChange capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.type()).isEqualTo(ChangeType.JOIN);
        assertThat(capturedEvent.watchingSession())
                .usingRecursiveComparison()
                .ignoringFields("watchingId", "createdAt")
                .isEqualTo(watchingSessionDto);
        assertThat(capturedEvent.watcherCount()).isEqualTo(3L);
    }

    @Test
    @DisplayName("leave 호출 시 Redis remove, count, repository 조회, 메시지 전송 모두 수행")
    void leaveShouldPerformAllSteps() {
        // given
        when(redisWatchingSessionRepository.removeWatcher(any(), any())).thenReturn(true);
        when(redisWatchingSessionRepository.countWatcher(any())).thenReturn(3L);

        // when
        watchingSessionService.leave(contentId);

        // then
        verify(redisWatchingSessionRepository, times(1))
                .removeWatcher(eq(contentId), any());
        verify(redisWatchingSessionRepository, times(1))
                .countWatcher(eq(contentId));
        verify(userRepository, times(1))
                .findById(any());
        verify(contentRepository, times(1))
                .findById(any());
        verify(tagRepository, times(1))
                .findByContentId(any());
        verify(publisher, times(1))
                .publish(eq(contentId), eventCaptor.capture());


        WatchingSessionChange capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.type()).isEqualTo(ChangeType.LEAVE);
        assertThat(capturedEvent.watchingSession())
                .usingRecursiveComparison()
                .ignoringFields("watchingId", "createdAt")
                .isEqualTo(watchingSessionDto);
        assertThat(capturedEvent.watcherCount()).isEqualTo(3L);
    }
}