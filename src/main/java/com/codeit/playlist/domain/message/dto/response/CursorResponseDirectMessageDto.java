package com.codeit.playlist.domain.message.dto.response;

import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.message.dto.data.DirectMessageDto;
import java.util.List;
import java.util.UUID;

public record CursorResponseDirectMessageDto(
    List<DirectMessageDto> data,
    String nextCursor,
    UUID nextIdAfter,
    boolean hasNext,
    long totalCount,
    String sortBy,
    SortDirection sortDirection

) {

}
