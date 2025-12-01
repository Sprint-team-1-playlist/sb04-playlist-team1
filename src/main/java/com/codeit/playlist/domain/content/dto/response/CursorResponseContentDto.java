package com.codeit.playlist.domain.content.dto.response;

import com.codeit.playlist.domain.base.SortDirection;

import java.util.List;

public record CursorResponseContentDto(
        List<Object> data,
        String nextCursor,
        String nextIdAfter,
        Boolean hasNext,
        Integer totalCount,
        String sortBy,
        SortDirection sortDirection
) {
}
