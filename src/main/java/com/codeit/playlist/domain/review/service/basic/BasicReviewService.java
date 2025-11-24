package com.codeit.playlist.domain.review.service.basic;

import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.content.entity.Content;
import com.codeit.playlist.domain.content.exception.ContentNotFoundException;
import com.codeit.playlist.domain.content.repository.ContentRepository;
import com.codeit.playlist.domain.review.dto.data.ReviewDto;
import com.codeit.playlist.domain.review.dto.request.ReviewCreateRequest;
import com.codeit.playlist.domain.review.dto.request.ReviewUpdateRequest;
import com.codeit.playlist.domain.review.dto.response.CursorResponseReviewDto;
import com.codeit.playlist.domain.review.entity.Review;
import com.codeit.playlist.domain.review.exception.ReviewAccessDeniedException;
import com.codeit.playlist.domain.review.exception.ReviewNotFoundException;
import com.codeit.playlist.domain.review.mapper.ReviewMapper;
import com.codeit.playlist.domain.review.repository.ReviewRepository;
import com.codeit.playlist.domain.review.service.ReviewService;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.exception.UserNotFoundException;
import com.codeit.playlist.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

    //리뷰 수정
    @Transactional
    @Override
    public ReviewDto updateReview(UUID reviewId, ReviewUpdateRequest request, UUID currentUserId) {
        log.debug("[리뷰] 수정 시작: reviewId={}, currentUserId={}", reviewId, currentUserId);

        //리뷰 조회
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> ReviewNotFoundException.withId(reviewId));

        //작성자 검증
        UUID authorId = review.getUser().getId();
        if (!authorId.equals(currentUserId)) {
            log.error("[리뷰] 수정 권한 없음: reviewId={}, authorId={}, currentUserId={}",
                    reviewId, authorId, currentUserId);
            throw ReviewAccessDeniedException.withId(reviewId);
        }

        review.updateReview(request.text(), request.rating());

        Review saved = reviewRepository.save(review);

        //DTO 변환
        ReviewDto dto = reviewMapper.toDto(saved);

        log.info("[리뷰] 리뷰 수정 성공: id= {}", reviewId);
        return dto;
    }

    //리뷰 조회
    @Transactional(readOnly = true)
    @Override
    public CursorResponseReviewDto findReviews(UUID contentId, String cursor,
                                               UUID idAfter, int limit,
                                               SortDirection sortDirection, String sortBy) {
        log.debug("[리뷰] 목록 조회 서비스 호출: " +
                        "contentId={}, cursor={}, idAfter={}, limit={}, sortDirection={}, sortBy={}",
                contentId, cursor, idAfter, limit, sortDirection, sortBy);

        //파라미터 보정
        if(limit <= 0 || limit > 50) {
            limit = 10; //기본 페이지 크기(10개 가져옴)
        }

        //sortBy 허용값(createdAt / rating)
        if (!"createdAt".equals(sortBy) && !"rating".equals(sortBy)) {
            sortBy = "createdAt";
        }

        //커서 해석 (cursor가 메인)
        UUID effectiveIdAfter = null;
        if (cursor != null && !cursor.isBlank()) {
            try {
                effectiveIdAfter = UUID.fromString(cursor);
            } catch (IllegalArgumentException e) {
                log.error("[리뷰] 잘못된 cursor 형식: cursor={}", cursor);
                effectiveIdAfter = null;
            }
        }

        //idAfter가 보조
        if (effectiveIdAfter == null && idAfter != null) {
            effectiveIdAfter = idAfter;
        }

        Slice<Review> reviews = reviewRepository.findReviews(
                contentId, cursor,
                effectiveIdAfter, limit,
                sortDirection, sortBy
        );

        //Entity -> DTO
        List<ReviewDto> data = reviews.getContent().stream()
                .map(reviewMapper::toDto)
                .toList();

        //nextCursor, nextIdAfter 계산
        String nextCursor = null;
        UUID nextIdAfter = null;

        if (reviews.hasNext() && !reviews.isEmpty()) {
            Review last = reviews.getContent().get(reviews.getNumberOfElements() - 1);
            nextIdAfter = last.getId();
            nextCursor = last.getId().toString(); // id를 커서로 사용
        }

        long totalCount;
        if (contentId != null) {
            totalCount = reviewRepository.countByContent_Id(contentId);
        } else {
            totalCount = reviewRepository.count();
        }

        CursorResponseReviewDto response = new CursorResponseReviewDto(
                data,
                nextCursor,
                nextIdAfter,
                reviews.hasNext(),
                totalCount,
                sortBy,
                sortDirection
        );

        log.info("[리뷰] 목록 조회 완료: contentId={}, 반환개수={}, hasNext={}",
                contentId, data.size(), reviews.hasNext());

        return response;
    }
}
