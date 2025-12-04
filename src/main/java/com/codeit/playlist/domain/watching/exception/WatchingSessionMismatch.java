package com.codeit.playlist.domain.watching.exception;

import java.util.UUID;

public class WatchingSessionMismatch extends WatchingException {
    public WatchingSessionMismatch() {
        super(WatchingErrorCode.WATCHING_SESSION_MISMATCH);
    }

    public static WatchingSessionMismatch withWatchingIdAndContentId(UUID watchingId, UUID contentId) {
        WatchingSessionMismatch exception = new WatchingSessionMismatch();
        exception.addDetail("watchingId", watchingId);
        exception.addDetail("contentId", contentId);
        return exception;
    }

    public static WatchingSessionMismatch withWatchingIdAndUserId(UUID watchingId, UUID userId) {
        WatchingSessionMismatch exception = new WatchingSessionMismatch();
        exception.addDetail("watchingId", watchingId);
        exception.addDetail("userId", userId);
        return exception;
    }

    public static WatchingSessionMismatch withPrincipalUserIdAndUserId(UUID userId, UUID principalUserId) {
        WatchingSessionMismatch exception = new WatchingSessionMismatch();
        exception.addDetail("userId", userId);
        exception.addDetail("principalUserId", principalUserId);
        return exception;
    }
}
