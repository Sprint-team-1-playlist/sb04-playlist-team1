package com.codeit.playlist.domain.content.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TheMovieTagResponse(
        @JsonProperty("id") Integer id,
        @JsonProperty("name") String name
) {
}
