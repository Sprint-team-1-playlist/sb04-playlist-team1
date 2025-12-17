package com.codeit.playlist.domain.content.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TheSportListResponse(
        @JsonProperty("events")
        List<TheSportResponse> events
) {
}
