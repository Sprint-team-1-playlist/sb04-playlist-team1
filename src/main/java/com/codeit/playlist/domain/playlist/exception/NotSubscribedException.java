package com.codeit.playlist.domain.playlist.exception;

import java.util.UUID;

public class NotSubscribedException extends PlaylistException {
    public NotSubscribedException() {
        super(PlaylistErrorCode.NOT_SUBSCRIBED);
    }

    public static NotSubscribedException withDetail(UUID playlistId, UUID subscriberId) {
        NotSubscribedException exception = new NotSubscribedException();
        exception.addDetail("playlistId", playlistId);
        exception.addDetail("subscriberId", subscriberId);
        return exception;
    }
}
