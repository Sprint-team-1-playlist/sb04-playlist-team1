package com.codeit.playlist.domain.watching.exception;

public class WatchingSessionUpdateException extends WatchingException{
    public WatchingSessionUpdateException() {
        super(WatchingErrorCode.WATCHING_SESSION_UPDATE_FAILED);
    }
}
