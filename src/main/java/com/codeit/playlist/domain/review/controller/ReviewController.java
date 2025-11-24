package com.codeit.playlist.domain.review.controller;

import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.review.dto.data.ReviewDto;
import com.codeit.playlist.domain.review.dto.request.ReviewCreateRequest;
import com.codeit.playlist.domain.review.dto.request.ReviewUpdateRequest;
import com.codeit.playlist.domain.review.dto.response.CursorResponseReviewDto;
import com.codeit.playlist.domain.review.service.ReviewService;
import com.codeit.playlist.domain.security.PlaylistUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    //리뷰 생성
    @PostMapping
    public ResponseEntity<ReviewDto> create(@Valid @RequestBody ReviewCreateRequest request,
                                            @AuthenticationPrincipal PlaylistUserDetails userDetails) {
        UUID reviewerId = userDetails.getUserDto().id();

        log.debug("[리뷰] 생성 요청: contentId = {}, reviewerId = {}", request.contentId(), reviewerId);

        ReviewDto review = reviewService.createReview(request, reviewerId);

        log.info("[리뷰] 생성 완료: id = {}", review.id());

        return ResponseEntity.status(HttpStatus.CREATED).body(review);
    }

    //리뷰 수정
    @PatchMapping("/{reviewId}")
    public ResponseEntity<ReviewDto> update(
            @PathVariable UUID reviewId,
            @Valid @RequestBody ReviewUpdateRequest request,
            @AuthenticationPrincipal PlaylistUserDetails userDetails) {

        UUID currentUserId = userDetails.getUserDto().id();
        log.debug("[리뷰] 수정 요청: reviewId = {}, currentUserId = {}", reviewId, currentUserId);

        ReviewDto updatedReview = reviewService.updateReview(reviewId, request, currentUserId);

        log.info("[리뷰] 수정 성공: id = {}", updatedReview.id());

        return ResponseEntity.ok(updatedReview);
    }

    //리뷰 목록 조회
    @GetMapping
    public ResponseEntity<CursorResponseReviewDto> geetReviewList(
            @RequestParam(required = false) UUID contentId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) UUID idAfter,
            @RequestParam int limit,
            @RequestParam(defaultValue = "DESCENDING") SortDirection sortDirection,  //DESCENDING, ASCENDING
            @RequestParam String sortBy  //createdAt, rating
    ) {

        log.debug("[리뷰] 리뷰 목록 조회 요청: " +
                        "contentId = {}, cursor = {}, idAfter = {}, limit = {},sortDirection = {}, sortBy = {}",
                contentId, cursor, idAfter, limit, sortDirection, sortBy);

        CursorResponseReviewDto reviews = reviewService.findReviews(
                contentId, cursor,
                idAfter, limit,
                sortDirection, sortBy
        );

        log.info("[리뷰] 리뷰 목록 조회 성공: contentId={}, 조회개수={}, nextCursor={}, hasNext={}",
                contentId, reviews.data().size(), reviews.nextCursor(), reviews.hasNext());

        return ResponseEntity.ok(reviews);
    }
}
