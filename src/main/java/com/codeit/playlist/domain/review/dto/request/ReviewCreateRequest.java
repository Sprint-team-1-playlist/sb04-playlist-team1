package com.codeit.playlist.domain.review.dto.request;

import java.util.UUID;

public record ReviewCreateRequest(
        UUID contentId,
        String text,
        int rating
) {
}
