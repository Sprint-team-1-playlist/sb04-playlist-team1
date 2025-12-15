package com.codeit.playlist.domain.content.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TvSeriesTagListResponse(
        @JsonProperty("genres") List<TvSeriesTagResponse> genres
) {
}
