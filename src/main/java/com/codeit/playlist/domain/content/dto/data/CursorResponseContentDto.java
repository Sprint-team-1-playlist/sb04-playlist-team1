package com.codeit.playlist.domain.content.dto.data;

import java.lang.reflect.Array;
import java.util.List;

public record CursorResponseContentDto(
        List<Object> data,
        String nextCursor,
        String nextIdAfter,
        Boolean hasNext,
        Integer totalCount,
        String sortBy,
        String sortDirection
) {
}
