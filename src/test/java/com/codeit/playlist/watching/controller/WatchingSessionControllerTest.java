package com.codeit.playlist.watching.controller;

import com.codeit.playlist.domain.security.PlaylistUserDetails;
import com.codeit.playlist.domain.user.dto.data.UserDto;
import com.codeit.playlist.domain.watching.controller.WatchingSessionController;
import com.codeit.playlist.domain.watching.dto.request.ContentChatSendRequest;
import com.codeit.playlist.domain.watching.service.WatchingSessionService;
import com.codeit.playlist.watching.fixture.WatchingSessionFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WatchingSessionControllerTest {
    @InjectMocks
    private WatchingSessionController webSocketController;

    @Mock
    private WatchingSessionService watchingSessionService;
    @Mock
    private Authentication authentication;
    @Mock
    private PlaylistUserDetails userDetails;

    private final UUID contentId = WatchingSessionFixtures.FIXED_ID;
    private final UserDto userDto = WatchingSessionFixtures.userDto();

    @BeforeEach
    void setUp() {
        // principal → authentication → playlistUserDetails → userDto
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUserDto()).thenReturn(userDto);
    }

    @Test
    @DisplayName("joinWatching 호출 시 watchingSessionService.join()이 호출됨")
    void joinWatchingShouldCallService() {
        // given
        UUID userId = userDto.id();

        // when
        webSocketController.joinWatching(contentId, authentication);

        // then
        verify(watchingSessionService, times(1)).join(contentId, userId);
    }

    @Test
    @DisplayName("leaveWatching 호출 시 watchingSessionService.leave()이 호출됨")
    void leaveWatchingShouldCallService() {
        // given
        UUID userId = userDto.id();

        // when
        webSocketController.leaveWatching(contentId, authentication);

        // then
        verify(watchingSessionService, times(1)).leave(contentId, userId);
    }

    @Test
    @DisplayName("sendChat 호출 시 watchingSessionService.sendChat()이 호출됨")
    void sendChatShouldCallService() {
        // given
        UUID userId = userDto.id();
        ContentChatSendRequest request = new ContentChatSendRequest("hello");

        // when
        webSocketController.sendChat(contentId, authentication, request);

        // then
        verify(watchingSessionService, times(1))
                .sendChat(contentId, userId, request);
    }
}