package com.codeit.playlist.domain.watching.exception;

import java.util.UUID;

public class EventBroadcastFailedException extends WatchingException {
    public EventBroadcastFailedException() {
        super(WatchingErrorCode.EVENT_BROADCAST_FAILED);
    }

    public static EventBroadcastFailedException withContentId(UUID id) {
        EventBroadcastFailedException exception = new EventBroadcastFailedException();
        exception.addDetail("contentId", id);
        return exception;
    }
}
