package com.codeit.playlist.domain.review.exception;

import java.util.UUID;

public class ReviewNotFoundException extends ReviewException {

    public ReviewNotFoundException() {
        super(ReviewErrorCode.REVIEW_NOT_FOUND);
    }

    public static ReviewNotFoundException withId(UUID reviewId) {
        ReviewNotFoundException exception = new ReviewNotFoundException();
        exception.addDetail("reviewId", reviewId);
        return exception;
    }
}
