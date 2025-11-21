package com.codeit.playlist.domain.review.dto.data;

import com.codeit.playlist.domain.user.dto.data.UserSummary;

import java.util.UUID;

public record ReviewDto(
        UUID id,
        UUID contentId,
        UserSummary author,
        String text,
        int rating
) {
}
