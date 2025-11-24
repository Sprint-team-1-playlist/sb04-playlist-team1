package com.codeit.playlist.domain.content.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TheSportsResponse(
        @JsonProperty("strEvent") String strEvent, // "strEvent": "UAB vs South Florida"
        @JsonProperty("strFilename") String strFilename, // "strFilename": "NCAA Division 1 2025-11-22 UAB vs South Florida"
        @JsonProperty("dateEventLocal") String dateEventLocal, // "dateEventLocal": "2025-11-21"
        @JsonProperty("strThumb") String strThumb // https://r2.thesportsdb.com/images/media/event/thumb/pu62u01694795331.jpg
) {
}
