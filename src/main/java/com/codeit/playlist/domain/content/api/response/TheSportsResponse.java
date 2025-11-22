package com.codeit.playlist.domain.content.api.response;

public record TheSportsResponse(
        String strEvent, // "strEvent": "UAB vs South Florida"
        String strFileName, // "strFilename": "NCAA Division 1 2025-11-22 UAB vs South Florida"
        String dateEventLocal // "dateEventLocal": "2025-11-21"
) {
}
