package com.codeit.playlist.domain.review.dto.request;

public record ReviewUpdateRequest(
        String text,
        int rating
) {
}
