package com.codeit.playlist.domain.content.exception;

import java.util.UUID;

public class ContentUnauthorizedException extends ContentException {
    public ContentUnauthorizedException() {
        super(ContentErrorCode.CONTENT_UNAUTHORIZED);
    }

    public static ContentUnauthorizedException withId(UUID contentId) {
        ContentUnauthorizedException exception = new ContentUnauthorizedException();
        exception.addDetail("contentId", contentId);
        return exception;
    }
}
