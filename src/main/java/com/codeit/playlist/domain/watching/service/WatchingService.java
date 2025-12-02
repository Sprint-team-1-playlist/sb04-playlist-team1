package com.codeit.playlist.domain.watching.service;

import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.watching.dto.data.WatchingSortBy;
import com.codeit.playlist.domain.watching.dto.data.WatchingSessionDto;
import com.codeit.playlist.domain.watching.dto.response.CursorResponseWatchingSessionDto;

import java.util.UUID;

public interface WatchingService {
    CursorResponseWatchingSessionDto getWatchingSessionsByContent(UUID contentId,
                                                                  String watcherNameLike,
                                                                  String cursor,
                                                                  UUID idAfter,
                                                                  int limit,
                                                                  SortDirection sortDirection,
                                                                  WatchingSortBy sortBy);

    WatchingSessionDto getWatchingSessionByUser(UUID userId);
}
