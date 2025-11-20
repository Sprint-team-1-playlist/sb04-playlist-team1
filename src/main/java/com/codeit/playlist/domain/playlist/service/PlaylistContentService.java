package com.codeit.playlist.domain.playlist.service;

import java.util.UUID;

public interface PlaylistContentService {
    void addContentToPlaylist(UUID playlistId, UUID contentId, UUID currentUserId);

    void removeContentFromPlaylist(UUID playlistId, UUID contentId, UUID currentUserId);
}
