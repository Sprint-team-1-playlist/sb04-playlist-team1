package com.codeit.playlist.watching.controller;

import com.codeit.playlist.domain.watching.controller.WebSocketController;
import com.codeit.playlist.domain.watching.service.basic.BasicWatchingSessionService;
import com.codeit.playlist.watching.fixture.WatchingSessionFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WebSocketControllerTest {
    @InjectMocks
    private WebSocketController webSocketController;

    @Mock
    private BasicWatchingSessionService watchingSessionService;

    private final UUID contentId = WatchingSessionFixtures.FIXED_ID;

    @Test
    @DisplayName("joinWatching 호출 시 watchingSessionService.join()이 호출됨")
    void joinWatchingShouldCallService() {
        // when
        webSocketController.joinWatching(contentId);

        // then
        verify(watchingSessionService, times(1)).join(contentId);
    }

    @Test
    @DisplayName("joinWatching 호출 시 watchingSessionService.leave()이 호출됨")
    void leaveWatchingShouldCallService() {
        // when
        webSocketController.leaveWatching(contentId);

        // then
        verify(watchingSessionService, times(1)).leave(contentId);
    }
}