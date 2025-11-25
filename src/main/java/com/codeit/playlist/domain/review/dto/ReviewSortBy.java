package com.codeit.playlist.domain.review.dto;

import lombok.Getter;

@Getter
public enum ReviewSortBy {
    CREATED_AT("createdAt"),
    RATING("rating");

    private final String value;

    ReviewSortBy(String value) {
        this.value = value;
    }
}
