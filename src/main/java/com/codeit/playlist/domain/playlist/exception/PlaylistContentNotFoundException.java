package com.codeit.playlist.domain.playlist.exception;

import java.util.UUID;

public class PlaylistContentNotFoundException extends PlaylistException {
    public PlaylistContentNotFoundException() {
        super(PlaylistErrorCode.PLAYLIST_CONTENT_NOT_FOUND);
    }

    public static PlaylistContentNotFoundException withIds(UUID playlistId, UUID contentId) {
      PlaylistContentNotFoundException exception = new PlaylistContentNotFoundException();
      exception.addDetail("playlistId", playlistId);
      exception.addDetail("contentId", contentId);
      return exception;
    }
}
