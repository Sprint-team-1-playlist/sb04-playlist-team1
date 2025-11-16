package com.codeit.playlist.domain.watching.dto.data;

import com.codeit.playlist.domain.content.dto.data.ContentDto;
import com.codeit.playlist.domain.user.dto.data.UserDto;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record WatchingSessionDto(
        UUID watchingId,
        LocalDateTime createdAt,
        UserDto watcher,
        ContentDto content
) {
}