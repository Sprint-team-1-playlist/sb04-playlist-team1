package com.codeit.playlist.watching.controller;

import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.watching.controller.WatchingController;
import com.codeit.playlist.domain.watching.dto.data.WatchingSessionDto;
import com.codeit.playlist.domain.watching.service.WatchingService;
import com.codeit.playlist.watching.fixture.WatchingSessionFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WatchingControllerTest {
    @InjectMocks
    private WatchingController watchingController;

    @Mock
    private WatchingService watchingService;

    @Test
    @DisplayName("getWatchingSessions 호출 시 watchingService.getWatchingSessions()이 호출됨")
    void getWatchingSessionsShouldCallService() {
        // given
        UUID contentId = WatchingSessionFixtures.FIXED_ID;
        String watcherNameLike = "test";
        String cursor = "12345";
        UUID idAfter = UUID.randomUUID();
        int limit = 10;
        SortDirection sortDirection = SortDirection.ASCENDING;
        String sortBy = "createdAt";

        // when
        watchingController.getWatchingSessionsByContent(
                contentId,
                watcherNameLike,
                cursor,
                idAfter,
                limit,
                sortDirection,
                sortBy);

        // then
        verify(watchingService, times(1))
                .getWatchingSessionsByContent(
                        contentId,
                        watcherNameLike,
                        cursor,
                        idAfter,
                        limit,
                        sortDirection,
                        sortBy);
    }

    @Test
    @DisplayName("getWatchingSessionByUser 호출 시 watchingService.getWatchingSessionByUser()가 호출됨")
    void getWatchingSessionByUserShouldCallService() {
        // given
        UUID userId = WatchingSessionFixtures.FIXED_ID;

        // when
        watchingController.getWatchingSessionByUser(userId);

        // then
        verify(watchingService, times(1))
                .getWatchingSessionByUser(userId);
    }

    @Test
    @DisplayName("getWatchingSessionByUser가 null을 반환하면 204 No Content가 리턴됨")
    void getWatchingSessionByUserShouldReturn204WhenNull() {
        // given
        UUID userId = WatchingSessionFixtures.FIXED_ID;

        when(watchingService.getWatchingSessionByUser(userId))
                .thenReturn(null);

        // when
        ResponseEntity<WatchingSessionDto> response = watchingController.getWatchingSessionByUser(userId);

        // then
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(watchingService, times(1))
                .getWatchingSessionByUser(userId);
    }
}