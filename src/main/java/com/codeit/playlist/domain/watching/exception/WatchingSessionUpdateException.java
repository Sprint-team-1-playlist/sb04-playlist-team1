package com.codeit.playlist.domain.watching.exception;

import java.util.UUID;

public class WatchingSessionUpdateException extends WatchingException {
    public WatchingSessionUpdateException() {
        super(WatchingErrorCode.WATCHING_SESSION_UPDATE_FAILED);
    }

    public static WatchingSessionUpdateException withId(UUID id) {
        WatchingSessionUpdateException exception = new WatchingSessionUpdateException();
        exception.addDetail("watchingId", id);
        return exception;
    }

    public static WatchingSessionUpdateException withContentIdUserId(UUID contentId, UUID userId) {
        WatchingSessionUpdateException exception = new WatchingSessionUpdateException();
        exception.addDetail("contentId", contentId);
        exception.addDetail("userId", userId);
        return exception;
    }

    public static WatchingSessionUpdateException watchingSessionId(String sessionId) {
        WatchingSessionUpdateException exception = new WatchingSessionUpdateException();
        exception.addDetail("sessionId", sessionId);
        return exception;
    }
}
