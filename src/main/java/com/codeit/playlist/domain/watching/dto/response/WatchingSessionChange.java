package com.codeit.playlist.domain.watching.dto.response;

import com.codeit.playlist.domain.watching.dto.data.ChangeType;
import com.codeit.playlist.domain.watching.dto.data.WatchingSessionDto;
import lombok.Builder;

@Builder
public record WatchingSessionChange(
        ChangeType type, // JOIN, LEAVE
        WatchingSessionDto watchingSession,
        long watcherCount
) {
}