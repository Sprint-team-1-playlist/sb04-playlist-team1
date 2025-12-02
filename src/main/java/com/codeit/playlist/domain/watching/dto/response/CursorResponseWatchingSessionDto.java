package com.codeit.playlist.domain.watching.dto.response;

import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.watching.dto.data.WatchingSortBy;
import com.codeit.playlist.domain.watching.dto.data.WatchingSessionDto;

import java.util.List;
import java.util.UUID;

public record CursorResponseWatchingSessionDto(
        List<WatchingSessionDto> watchingSessions,
        String nextCursor,
        UUID nextIdAfter,
        boolean hasNext,
        long totalCount,
        WatchingSortBy sortBy,
        SortDirection sortDirection
) {
}
