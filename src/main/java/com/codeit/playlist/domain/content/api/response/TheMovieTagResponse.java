package com.codeit.playlist.domain.content.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TheMovieTagResponse(
        @JsonProperty("id") Integer genreId,
        @JsonProperty("name") String name
) {
}
