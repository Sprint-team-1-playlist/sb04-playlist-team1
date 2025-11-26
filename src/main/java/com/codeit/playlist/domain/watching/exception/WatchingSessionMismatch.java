package com.codeit.playlist.domain.watching.exception;

import java.util.UUID;

public class WatchingSessionMismatch extends WatchingException {
    public WatchingSessionMismatch() {
        super(WatchingErrorCode.WATCHING_SESSION_MISMATCH);
    }

    public static WatchingSessionMismatch withId(UUID id) {
        WatchingSessionMismatch exception = new WatchingSessionMismatch();
        exception.addDetail("watchingId", id);
        return exception;
    }
}
