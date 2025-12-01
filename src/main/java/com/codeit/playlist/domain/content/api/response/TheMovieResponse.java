package com.codeit.playlist.domain.content.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TheMovieResponse(
        @JsonProperty("title") String title,
        @JsonProperty("overview") String description,
        @JsonProperty("backdrop_path") String thumbnailUrl,
        @JsonProperty("vote_average") Double averageRating
) {
}
