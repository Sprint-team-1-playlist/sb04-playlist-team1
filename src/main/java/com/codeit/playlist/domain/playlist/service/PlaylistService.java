package com.codeit.playlist.domain.playlist.service;

import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.playlist.dto.data.PlaylistDto;
import com.codeit.playlist.domain.playlist.dto.data.PlaylistSortBy;
import com.codeit.playlist.domain.playlist.dto.request.PlaylistCreateRequest;
import com.codeit.playlist.domain.playlist.dto.request.PlaylistUpdateRequest;
import com.codeit.playlist.domain.playlist.dto.response.CursorResponsePlaylistDto;

import java.util.UUID;

public interface PlaylistService {
    PlaylistDto createPlaylist(PlaylistCreateRequest request, UUID ownerId);

    PlaylistDto updatePlaylist(UUID playlistId, PlaylistUpdateRequest request, UUID currentUserId);

    void softDeletePlaylist(UUID playlistId, UUID requesterUserId);

    void deletePlaylist(UUID playlistId, UUID requesterUserId);

    CursorResponsePlaylistDto findPlaylists(String keywordLike, UUID ownerIdEqual,
                                            UUID subscriberIdEqual, String cursor,
                                            UUID idAfter, int limit, PlaylistSortBy sortBy,
                                            SortDirection sortDirection);

    PlaylistDto getPlaylist(UUID playlistId);
}
