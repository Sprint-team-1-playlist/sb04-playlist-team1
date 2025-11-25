package com.codeit.playlist.domain.review.repository.custom;

import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.review.entity.Review;
import org.springframework.data.domain.Slice;

import java.util.UUID;

public interface ReviewRepositoryCustom {

    Slice<Review> findReviews(
            UUID contentId,
            String cursor,
            UUID idAfter,
            int limit,
            SortDirection sortDirection,  // ASCENDING / DESCENDING
            String sortBy                // createdAt / rating
    );
}
