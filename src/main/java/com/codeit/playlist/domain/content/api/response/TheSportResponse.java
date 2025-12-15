package com.codeit.playlist.domain.content.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TheSportResponse(
    @JsonProperty("idEvent") String idEvent, // id
    @JsonProperty("strEvent") String strEvent, // 이름
    @JsonProperty("strFilename") String strFilename, // 설명
    @JsonProperty("strSport") String strSport, // 태그
    @JsonProperty("strHomeTeam") String strHomeTeam, // 태그2
    @JsonProperty("dateEvent") String dateEvent,
    @JsonProperty("strPoster") String strPoster
) {
}
