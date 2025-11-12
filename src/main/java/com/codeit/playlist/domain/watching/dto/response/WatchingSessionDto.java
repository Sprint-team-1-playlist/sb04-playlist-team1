package com.codeit.playlist.domain.watching.dto.response;

import com.codeit.playlist.domain.content.dto.data.ContentDto;
import com.codeit.playlist.domain.user.dto.data.UserDto;

import java.time.LocalDateTime;
import java.util.UUID;

public record WatchingSessionDto(
        UUID watchingId,
        LocalDateTime createdAt,
        UserDto watcher,
        ContentDto content
) {
}
