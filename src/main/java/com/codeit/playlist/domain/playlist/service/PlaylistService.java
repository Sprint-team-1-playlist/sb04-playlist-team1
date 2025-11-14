package com.codeit.playlist.domain.playlist.service;

import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.playlist.dto.data.PlaylistDto;
import com.codeit.playlist.domain.playlist.dto.request.PlaylistCreateRequest;
import com.codeit.playlist.domain.playlist.dto.request.PlaylistUpdateRequest;
import com.codeit.playlist.domain.playlist.dto.response.CursorResponsePlaylistDto;

import java.util.UUID;

public interface PlaylistService {
    PlaylistDto createPlaylist(PlaylistCreateRequest request, UUID ownerId);

    PlaylistDto updatePlaylist(UUID playlistId, PlaylistUpdateRequest request);

//    void deletePlaylist(UUID playlistId); //플레이리스트 삭제 서비스 코드 임시 비활성화

    CursorResponsePlaylistDto findPlaylists(String keywordLike, UUID ownerIdEqual,
                                            UUID subscriberIdEqual, String cursor,
                                            UUID idAfter, int limit, String sortBy,
                                            SortDirection sortDirection);
}
