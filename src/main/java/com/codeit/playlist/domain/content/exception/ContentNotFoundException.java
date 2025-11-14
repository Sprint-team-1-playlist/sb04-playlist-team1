package com.codeit.playlist.domain.content.exception;


import java.util.UUID;

public class ContentNotFoundException extends ContentException {
    public ContentNotFoundException() {
        super(ContentErrorCode.CONTENT_NOT_FOUND);
    }

    public static ContentNotFoundException withId(UUID contentId) {
        ContentNotFoundException exception = new ContentNotFoundException();
        exception.addDetail("contentId", contentId);
        return exception;
    }
}
