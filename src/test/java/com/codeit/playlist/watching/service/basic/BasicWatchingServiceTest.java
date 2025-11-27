package com.codeit.playlist.watching.service.basic;

import com.codeit.playlist.domain.content.entity.Content;
import com.codeit.playlist.domain.content.entity.Tag;
import com.codeit.playlist.domain.content.mapper.ContentMapper;
import com.codeit.playlist.domain.content.repository.ContentRepository;
import com.codeit.playlist.domain.content.repository.TagRepository;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.mapper.UserMapper;
import com.codeit.playlist.domain.user.repository.UserRepository;
import com.codeit.playlist.domain.watching.dto.data.RawWatchingSession;
import com.codeit.playlist.domain.watching.dto.data.RawWatchingSessionPage;
import com.codeit.playlist.domain.watching.dto.data.WatchingSessionDto;
import com.codeit.playlist.domain.watching.dto.request.WatchingSessionRequest;
import com.codeit.playlist.domain.watching.dto.response.CursorResponseWatchingSessionDto;
import com.codeit.playlist.domain.watching.repository.RedisWatchingSessionRepository;
import com.codeit.playlist.domain.watching.service.basic.BasicWatchingService;
import com.codeit.playlist.watching.fixture.WatchingSessionFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BasicWatchingServiceTest {
    @InjectMocks
    private BasicWatchingService watchingService;

    @Mock
    private RedisWatchingSessionRepository redisWatchingSessionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private ContentRepository contentRepository;
    @Mock
    private ContentMapper contentMapper;
    @Mock
    private TagRepository tagRepository;

    private final UUID contentId = WatchingSessionFixtures.FIXED_ID;
    private final WatchingSessionRequest request = WatchingSessionFixtures.watchingSessionRequest();
    private final RawWatchingSession raw = WatchingSessionFixtures.rawWatchingSession();
    private final RawWatchingSessionPage rawPage = WatchingSessionFixtures.rawWatchingSessionPage();

    @Test
    @DisplayName("getWatchingSessions 호출 시 RedisWatchingSessionRepository.getWatchingSessions가 호출된다")
    void getWatchingSessions_callsRepository() {
        // given
        when(redisWatchingSessionRepository.getWatchingSessions(contentId, request))
                .thenReturn(rawPage);
        when(redisWatchingSessionRepository.countWatchingSessionByContentId(contentId))
                .thenReturn(10L);

        User user = WatchingSessionFixtures.user();
        Content content = WatchingSessionFixtures.content();
        List<Tag> tags = WatchingSessionFixtures.tagList();

        when(userRepository.findById(raw.userId())).thenReturn(Optional.of(user));
        when(contentRepository.findById(raw.contentId())).thenReturn(Optional.of(content));
        when(tagRepository.findByContentId(raw.contentId())).thenReturn(tags);

        WatchingSessionDto watchingSessionDto = WatchingSessionFixtures.watchingSessionDto();
        when(userMapper.toDto(user)).thenReturn(WatchingSessionFixtures.userDto());
        when(contentMapper.toDto(content, tags)).thenReturn(watchingSessionDto.content());

        // when
        CursorResponseWatchingSessionDto response = watchingService.getWatchingSessions(contentId, request);

        // then
        verify(redisWatchingSessionRepository, times(1))
                .getWatchingSessions(contentId, request);
        verify(redisWatchingSessionRepository, times(1))
                .countWatchingSessionByContentId(contentId);

        verify(userRepository, times(1)).findById(raw.userId());
        verify(contentRepository, times(1)).findById(raw.contentId());
        verify(tagRepository, times(1)).findByContentId(raw.contentId());

        assertThat(response.watchingSessions()).hasSize(1);
        assertThat(response.totalCount()).isEqualTo(10L);
        assertThat(response.nextCursor()).isNotNull();
        assertThat(response.nextIdAfter()).isNotNull();
        assertThat(response.hasNext()).isEqualTo(rawPage.hasNext());
    }
}