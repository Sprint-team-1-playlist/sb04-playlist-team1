package com.codeit.playlist.domain.content.exception;

import java.util.UUID;

public class ContentInternalErrorException extends ContentException {
    public ContentInternalErrorException() {
        super(ContentErrorCode.CONTENT_INTERNAL_SERVER_ERROR);
    }

    public static ContentInternalErrorException getId(UUID contentId) {
        ContentInternalErrorException exception = new ContentInternalErrorException();
        exception.addDetail("contentId", contentId);
        return exception;
    }
}
