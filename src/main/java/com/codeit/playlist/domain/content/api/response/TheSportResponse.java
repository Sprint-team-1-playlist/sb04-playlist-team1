package com.codeit.playlist.domain.content.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TheSportResponse(
    @JsonProperty("id") String id,
    @JsonProperty("idEvent") String idEvent,
    @JsonProperty("inDivision") String inDivision,
    @JsonProperty("strSport") String strSport,
    @JsonProperty("strEvent") String strEvent,
    @JsonProperty("idChannel") String idChannel,
    @JsonProperty("strEventCountry") String strEventCountry,
    @JsonProperty("strLogo") String strLogo,
    @JsonProperty("strChannel") String strChannel,
    @JsonProperty("strSeason") String strSeason,
    @JsonProperty("strTime") String strTime,
    @JsonProperty("dateEvent") String dateEvent,
    @JsonProperty("strTimeStamp") String strTimeStamp
) {
}
