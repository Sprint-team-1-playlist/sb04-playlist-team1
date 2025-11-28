package com.codeit.playlist.watching.controller;

import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.watching.controller.WatchingController;
import com.codeit.playlist.domain.watching.dto.data.SortBy;
import com.codeit.playlist.domain.watching.service.WatchingService;
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
        SortBy sortBy = SortBy.createdAt;

        // when
        watchingController.getWatchingSessions(
                contentId,
                watcherNameLike,
                cursor,
                idAfter,
                limit,
                sortDirection,
                sortBy);

        // then
        verify(watchingService, times(1))
                .getWatchingSessions(
                        contentId,
                        watcherNameLike,
                        cursor,
                        idAfter,
                        limit,
                        sortDirection,
                        sortBy);
    }
}