package com.codeit.playlist.domain.playlist.exception;

import java.util.UUID;

public class AlreadySubscribedException extends PlaylistException {
    public AlreadySubscribedException() {
        super(PlaylistErrorCode.ALREADY_SUBSCRIBED);
    }

    public static AlreadySubscribedException withDetail(UUID playlistId, UUID subscriberId) {
        AlreadySubscribedException exception = new AlreadySubscribedException();
        exception.addDetail("playlistId", playlistId);
        exception.addDetail("subscriberId", subscriberId);
        return exception;
    }
}
