package com.codeit.playlist.domain.content.exception;

import java.util.UUID;

public class ContentBadRequestException extends ContentException {
    public ContentBadRequestException() {
        super(ContentErrorCode.CONTENT_BAD_REQUEST);
    }

    public static ContentBadRequestException withId(UUID contentId) {
        ContentBadRequestException exception = new ContentBadRequestException();
        exception.addDetail("contentId", contentId);
        return exception;
    }
}
