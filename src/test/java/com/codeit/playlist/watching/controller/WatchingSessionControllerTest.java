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

import java.security.Principal;
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
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUserDto()).thenReturn(userDto);
    }

    @Test
    @DisplayName("sendChat 호출 시 watchingSessionService.sendChat()이 호출됨")
    void sendChatShouldCallService() {
        // given
        UUID userId = userDto.id();
        Principal principal = authentication;
        ContentChatSendRequest request = new ContentChatSendRequest("hello");

        // when
        webSocketController.sendChat(contentId, principal, request);

        // then
        verify(watchingSessionService, times(1))
                .sendChat(contentId, userId, request);
    }
}