package com.codeit.playlist.domain.playlist.exception;

import java.util.UUID;

public class PlaylistNotFoundException extends PlaylistException {

    public PlaylistNotFoundException() {
        super(PlaylistErrorCode.PLAYLIST_NOT_FOUND);
    }

    public static PlaylistNotFoundException withId(UUID playlistId) {
        PlaylistNotFoundException exception = new PlaylistNotFoundException();
        exception.addDetail("playlistId", playlistId);
        return exception;
    }
}
