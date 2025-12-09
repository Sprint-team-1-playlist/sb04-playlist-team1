package com.codeit.playlist.domain.playlist.dto.data;

import com.codeit.playlist.domain.content.dto.data.ContentSummary;
import com.codeit.playlist.domain.user.dto.data.UserSummary;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PlaylistDto(
        UUID id,
        UserSummary owner,
        String title,
        String description,
        Instant updatedAt,
        Long subscriberCount,
        Boolean subscribedByMe,
        List<ContentSummary> contents
) {
}
