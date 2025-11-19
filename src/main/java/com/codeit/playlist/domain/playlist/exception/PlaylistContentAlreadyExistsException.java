package com.codeit.playlist.domain.playlist.exception;

import java.util.UUID;

public class PlaylistContentAlreadyExistsException extends PlaylistException {
    public PlaylistContentAlreadyExistsException() {
        super(PlaylistErrorCode.PLAYLIST_CONTENT_ALREADY_EXISTS);
    }

    public PlaylistContentAlreadyExistsException withIds(UUID playlistId, UUID contentId) {
        PlaylistContentAlreadyExistsException exception = new PlaylistContentAlreadyExistsException();
        exception.addDetail("playlistId", playlistId.toString());
        exception.addDetail("contentId", contentId.toString());
        return exception;
    }
}
