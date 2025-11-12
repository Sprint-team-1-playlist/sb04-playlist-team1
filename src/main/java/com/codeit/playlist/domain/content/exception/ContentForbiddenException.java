package com.codeit.playlist.domain.content.exception;

import java.util.UUID;

public class ContentForbiddenException extends ContentException {
    public ContentForbiddenException() {
        super(ContentErrorCode.CONTENT_FORBIDDEN);
    }

    public static ContentForbiddenException withId(UUID contentId) {
        ContentForbiddenException exception = new ContentForbiddenException();
        exception.addDetail("contentId", contentId);
        return exception;
    }
}
