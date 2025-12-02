package com.codeit.playlist.domain.playlist.dto.response;

import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.playlist.dto.data.PlaylistDto;
import com.codeit.playlist.domain.playlist.dto.data.PlaylistSortBy;

import java.util.List;
import java.util.UUID;

public record CursorResponsePlaylistDto(
        List<PlaylistDto> data,
        String nextCursor,
        UUID nextIdAfter,
        Boolean hasNext,
        Long totalCount,
        PlaylistSortBy sortBy,
        SortDirection sortDirection
) {
}
