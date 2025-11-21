package com.codeit.playlist.domain.review.exception;

import java.util.UUID;

public class ReviewAccessDeniedException extends ReviewException {
    public ReviewAccessDeniedException() {
        super(ReviewErrorCode.REVIEW_ACCESS_DENIED);
    }

    public static ReviewAccessDeniedException withId(UUID reviewId) {
      ReviewAccessDeniedException exception = new ReviewAccessDeniedException();
      exception.addDetail("reviewId", reviewId);
      return exception;
    }
}
