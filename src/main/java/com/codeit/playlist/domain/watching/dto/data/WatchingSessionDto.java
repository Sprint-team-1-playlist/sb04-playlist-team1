package com.codeit.playlist.domain.watching.dto.data;

import com.codeit.playlist.domain.content.dto.data.ContentDto;
import com.codeit.playlist.domain.user.dto.data.UserSummary;

import java.time.Instant;
import java.util.UUID;

public record WatchingSessionDto(
        UUID id,
        Instant createdAt,
        UserSummary watcher,
        ContentDto content
) {
}