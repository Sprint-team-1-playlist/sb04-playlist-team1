package com.codeit.playlist.domain.notification.dto.data;

import com.codeit.playlist.domain.notification.entity.Level;

import java.time.Instant;
import java.util.UUID;

public record NotificationDto(
        UUID id,
        Instant createdAt,
        UUID receiverId,
        String title,
        String content,
        Level level
) {
}
