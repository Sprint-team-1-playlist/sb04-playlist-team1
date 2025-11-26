package com.codeit.playlist.domain.watching.dto.request;

import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.watching.dto.data.SortBy;

import java.util.UUID;

public record WatchingSessionRequest(
        String watcherNameLike,
        String cursor,
        UUID idAfter,
        int limit,
        SortDirection sortDirection,
        SortBy sortBy
) {
}
