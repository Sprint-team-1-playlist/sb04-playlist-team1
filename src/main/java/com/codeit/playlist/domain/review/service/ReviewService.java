package com.codeit.playlist.domain.review.service;

import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.review.dto.ReviewSortBy;
import com.codeit.playlist.domain.review.dto.data.ReviewDto;
import com.codeit.playlist.domain.review.dto.request.ReviewCreateRequest;
import com.codeit.playlist.domain.review.dto.request.ReviewUpdateRequest;
import com.codeit.playlist.domain.review.dto.response.CursorResponseReviewDto;

import java.util.UUID;

public interface ReviewService {
    ReviewDto createReview(ReviewCreateRequest request, UUID reviewerId);

    ReviewDto updateReview(UUID reviewId, ReviewUpdateRequest request, UUID currentUserId);

    CursorResponseReviewDto findReviews(UUID contentId, String cursor,
                                        UUID idAfter, int limit,
                                        SortDirection sortDirection, ReviewSortBy sortBy);

    void deleteReview(UUID reviewId, UUID currentUserId);
}
