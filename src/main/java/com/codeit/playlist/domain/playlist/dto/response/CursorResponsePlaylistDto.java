package com.codeit.playlist.domain.playlist.dto.response;

import com.codeit.playlist.domain.playlist.dto.data.PlaylistDto;

import java.util.List;
import java.util.UUID;

public record CursorResponsePlaylistDto(
        List<PlaylistDto> data,
        String nextCursor,
        UUID nextIdAfter,
        Boolean hasNext,
        Long totalCount,
        String sortBy
//        ,SortDirection sortDirection
) {
}
