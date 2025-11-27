package com.codeit.playlist.domain.watching.dto.request;

import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.watching.dto.data.SortBy;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record WatchingSessionRequest(
        @NotBlank(message = "사용자 이름 필수")
        String watcherNameLike,

        String cursor,

        UUID idAfter,

        @Min(value = 1, message = "limit은 최소 1 이상")
        int limit,

        @NotNull(message = "정렬 방향 필수")
        SortDirection sortDirection,

        @NotNull(message = "정렬 기준 필수")
        SortBy sortBy
) {
}
