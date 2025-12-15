package com.codeit.playlist.domain.content.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TvSeriesResponse(
    @JsonProperty("results") List<TvSeriesResponse> results,
    @JsonProperty("page") int page,
    @JsonProperty("total_pages") int totalPages,
    @JsonProperty("total_results") int totalResults
) {
}
