package com.codeit.playlist.domain.notification.dto.response;

import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.notification.dto.data.NotificationDto;

import java.util.List;
import java.util.UUID;

public record CursorResponseNotificationDto(
        List<NotificationDto> data,
        String nextCursor,
        UUID nextIdAfter,
        Boolean hasNext,
        Long totalCount,
        String sortBy,
        SortDirection sortDirection
) {
}
