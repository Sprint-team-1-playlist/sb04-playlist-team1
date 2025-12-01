package com.codeit.playlist.domain.watching.dto.request;

import com.codeit.playlist.domain.base.SortDirection;

import java.util.UUID;

public record WatchingSessionRequest(
        UUID contentId,
        String watcherNameLike,
        String cursor,
        UUID idAfter,
        int limit,
        SortDirection sortDirection,
        String sortBy
) {
}
