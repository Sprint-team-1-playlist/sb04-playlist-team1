package com.codeit.playlist.domain.content.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TheMovieResponse(
        @JsonProperty("title") String title,
        @JsonProperty("overview") String description,
        @JsonProperty("poster_path") String thumbnailUrl,
        @JsonProperty("vote_average") Double averageRating,
        @JsonProperty("genre_ids") List<Integer> genreIds
) {
}
