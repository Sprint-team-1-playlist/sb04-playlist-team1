package com.codeit.playlist.domain.playlist.exception;

import java.util.UUID;

public class PlaylistContentAlreadyExistsException extends PlaylistException {
    public PlaylistContentAlreadyExistsException() {
        super(PlaylistErrorCode.PLAYLIST_CONTENT_ALREADY_EXISTS);
    }

    public PlaylistContentAlreadyExistsException(UUID playlistId, UUID contentId) {
        super(PlaylistErrorCode.PLAYLIST_CONTENT_ALREADY_EXISTS);
        this.addDetail("playlistId", playlistId.toString());
        this.addDetail("contentId", contentId.toString());
    }
}
