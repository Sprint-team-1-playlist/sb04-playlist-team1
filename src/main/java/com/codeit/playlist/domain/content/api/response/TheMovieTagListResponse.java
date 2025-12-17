package com.codeit.playlist.domain.content.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TheMovieTagListResponse(
        @JsonProperty("genres")List<TheMovieTagResponse> genres
) {
}
