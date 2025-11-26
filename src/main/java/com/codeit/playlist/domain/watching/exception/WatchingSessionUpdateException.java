package com.codeit.playlist.domain.watching.exception;

import java.util.UUID;

public class WatchingSessionUpdateException extends WatchingException{
    public WatchingSessionUpdateException() {
        super(WatchingErrorCode.WATCHING_SESSION_UPDATE_FAILED);
    }

    public static WatchingSessionUpdateException withId(UUID id) {
        WatchingSessionUpdateException exception = new WatchingSessionUpdateException();
        exception.addDetail("watchingId", id);
        return exception;
    }
}
