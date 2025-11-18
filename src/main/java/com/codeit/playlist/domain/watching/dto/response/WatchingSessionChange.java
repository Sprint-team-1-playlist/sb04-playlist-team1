package com.codeit.playlist.domain.watching.dto.response;

import com.codeit.playlist.domain.watching.dto.data.ChangeType;
import com.codeit.playlist.domain.watching.dto.data.WatchingSessionDto;

public record WatchingSessionChange(
        ChangeType type, // JOIN, LEAVE
        WatchingSessionDto watchingSession,
        long watcherCount
) {
}