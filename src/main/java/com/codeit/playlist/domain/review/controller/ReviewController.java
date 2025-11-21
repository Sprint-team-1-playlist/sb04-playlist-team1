package com.codeit.playlist.domain.review.controller;

import com.codeit.playlist.domain.review.dto.data.ReviewDto;
import com.codeit.playlist.domain.review.dto.request.ReviewCreateRequest;
import com.codeit.playlist.domain.review.service.ReviewService;
import com.codeit.playlist.domain.security.PlaylistUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

}
