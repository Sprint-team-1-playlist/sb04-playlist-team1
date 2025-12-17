package com.codeit.playlist.domain.watching.exception;

import java.util.UUID;

public class WatchingNotFoundException extends WatchingException {
    public WatchingNotFoundException() {
        super(WatchingErrorCode.WATCHING_NOT_FOUND);
    }

    public static WatchingNotFoundException withId(UUID id) {
        WatchingNotFoundException exception = new WatchingNotFoundException();
        exception.addDetail("watchingId", id);
        return exception;
    }

    public static WatchingNotFoundException withSessionId(String sessionId) {
        WatchingNotFoundException exception = new WatchingNotFoundException();
        exception.addDetail("watchingSessionId", sessionId);
        return exception;
    }
}
