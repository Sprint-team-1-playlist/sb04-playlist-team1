package com.codeit.playlist.domain.playlist.exception;

import java.util.UUID;

public class PlaylistAccessDeniedException extends PlaylistException {
    public PlaylistAccessDeniedException() {
        super(PlaylistErrorCode.PLAYLIST_ACCESS_DENIED);
    }

    public static PlaylistAccessDeniedException withPlaylistId(UUID playlistId) {
        PlaylistAccessDeniedException exception = new PlaylistAccessDeniedException();
        exception.addDetail("playlistId", playlistId);
        return exception;
    }

    //playlistId + 유저 정보
    public static PlaylistAccessDeniedException withIds(UUID playlistId, UUID ownerId, UUID currentUserId) {
        PlaylistAccessDeniedException exception = new PlaylistAccessDeniedException();
        exception.addDetail("playlistId", playlistId);
        exception.addDetail("ownerId", ownerId);
        exception.addDetail("currentUserId", currentUserId);
        return exception;
    }
}
