package com.codeit.playlist.domain.playlist.service;

import com.codeit.playlist.domain.playlist.dto.data.PlaylistDto;
import com.codeit.playlist.domain.playlist.dto.request.PlaylistCreateRequest;

import java.util.UUID;

public interface PlaylistService {
    PlaylistDto createPlaylist(PlaylistCreateRequest request, UUID ownerId);
}
