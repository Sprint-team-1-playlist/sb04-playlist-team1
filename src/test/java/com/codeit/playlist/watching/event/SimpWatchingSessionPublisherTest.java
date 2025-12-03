package com.codeit.playlist.watching.event;

import com.codeit.playlist.domain.watching.dto.response.WatchingSessionChange;
import com.codeit.playlist.domain.watching.event.impl.SimpWatchingSessionPublisher;
import com.codeit.playlist.watching.fixture.WatchingSessionFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SimpWatchingSessionPublisherTest {
    @InjectMocks
    private SimpWatchingSessionPublisher publisher;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Test
    @DisplayName("publish 호출 시 contentId와 WatchingSessionChange 전달 수행")
    void publishWatchingShouldCallConverterAndSend() {
        // given
        String destinationPattern = "/sub/contents/%s/watch";
        UUID contentId = WatchingSessionFixtures.FIXED_ID;
        WatchingSessionChange event = WatchingSessionFixtures.watchingSessionChange();

        // when
        publisher.publishWatching(contentId, event);

        // then
        verify(messagingTemplate)
                .convertAndSend(String.format(destinationPattern, contentId), event);
    }
}