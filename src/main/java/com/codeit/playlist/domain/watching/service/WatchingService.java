package com.codeit.playlist.domain.watching.service;

import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.watching.dto.data.SortBy;
import com.codeit.playlist.domain.watching.dto.response.CursorResponseWatchingSessionDto;

import java.util.UUID;

public interface WatchingService {
    public CursorResponseWatchingSessionDto getWatchingSessions(UUID contentId,
                                                                String watcherNameLike,
                                                                String cursor,
                                                                UUID idAfter,
                                                                int limit,
                                                                SortDirection sortDirection,
                                                                SortBy sortBy);
}
