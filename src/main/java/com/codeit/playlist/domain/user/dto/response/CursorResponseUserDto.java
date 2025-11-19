package com.codeit.playlist.domain.user.dto.response;

import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.user.dto.data.UserDto;
import java.util.List;
import java.util.UUID;

public record CursorResponseUserDto(
    List<UserDto> data,
    String nextCursor,
    UUID nextIdAfter,
    boolean hasNext,
    long totalCount,
    String sortBy,
    SortDirection sortDirection
) {
}
