package com.codeit.playlist.domain.review.service;

import com.codeit.playlist.domain.review.dto.data.ReviewDto;
import com.codeit.playlist.domain.review.dto.request.ReviewCreateRequest;
import com.codeit.playlist.domain.review.dto.request.ReviewUpdateRequest;

import java.util.UUID;

public interface ReviewService {
    ReviewDto createReview(ReviewCreateRequest request, UUID reviewerId);

    ReviewDto updateReview(UUID reviewId, ReviewUpdateRequest request, UUID currentUserId);

}
