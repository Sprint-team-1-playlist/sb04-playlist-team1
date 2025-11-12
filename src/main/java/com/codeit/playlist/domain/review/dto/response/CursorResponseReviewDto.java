package com.codeit.playlist.domain.review.dto.response;

import com.codeit.playlist.domain.review.dto.data.ReviewDto;

import java.util.List;
import java.util.UUID;

public record CursorResponseReviewDto(
        List<ReviewDto> data,
        String nextCursor,
        UUID nextIdAfter,
        Boolean hasNext,
        Long totalCount,
        String sortBy
//        , SortDirection sortDirection
) {
}
