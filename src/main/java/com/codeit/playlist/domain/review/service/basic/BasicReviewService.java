package com.codeit.playlist.domain.review.service.basic;

import com.codeit.playlist.domain.content.entity.Content;
import com.codeit.playlist.domain.content.exception.ContentNotFoundException;
import com.codeit.playlist.domain.content.repository.ContentRepository;
import com.codeit.playlist.domain.review.dto.data.ReviewDto;
import com.codeit.playlist.domain.review.dto.request.ReviewCreateRequest;
import com.codeit.playlist.domain.review.entity.Review;
import com.codeit.playlist.domain.review.mapper.ReviewMapper;
import com.codeit.playlist.domain.review.repository.ReviewRepository;
import com.codeit.playlist.domain.review.service.ReviewService;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.exception.UserNotFoundException;
import com.codeit.playlist.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BasicReviewService implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewMapper reviewMapper;
    private final UserRepository userRepository;
    private final ContentRepository contentRepository;

    //리뷰 생성
    @Transactional
    @Override
    public ReviewDto createReview(ReviewCreateRequest request, UUID reviewerId) {

        log.debug("[리뷰] 생성 요청: reviewerId= {}, contentId= {}", reviewerId, request.contentId());

        //작성자 조회
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> UserNotFoundException.withId(reviewerId));

        //컨텐츠 조회
        Content content = contentRepository.findById(request.contentId())
                .orElseThrow(() -> ContentNotFoundException.withId(request.contentId()));

        //엔티티 생성
        Review review = reviewMapper.toEntity(request, content, reviewer);

        //저장
        Review saved = reviewRepository.save(review);

        ReviewDto dto = reviewMapper.toDto(saved);

        log.info("[리뷰] 생성 완료: id={}", dto.id());
        return dto;
    }
}
