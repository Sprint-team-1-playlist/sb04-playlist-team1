package com.codeit.playlist.domain.review.service.basic;

import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.content.entity.Content;
import com.codeit.playlist.domain.content.exception.ContentNotFoundException;
import com.codeit.playlist.domain.content.repository.ContentRepository;
import com.codeit.playlist.domain.review.dto.ReviewSortBy;
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
                .orElseThrow(() -> {
                    log.error("[리뷰] 생성 실패 : 사용자를 찾을 수 없습니다. reviewerId= {}", reviewerId);
                    return UserNotFoundException.withId(reviewerId);
                });

        //컨텐츠 조회
        Content content = contentRepository.findById(request.contentId())
                .orElseThrow(() -> {
                    log.error("[리뷰] 생성 실패 : 컨텐츠를 찾을 수 없습니다. request.contentId= {}", request.contentId());
                    return ContentNotFoundException.withId(request.contentId());
                });

        //엔티티 생성
        Review review = reviewMapper.toEntity(request, content, reviewer);

        int newRating = review.getRating();

        // 콘텐츠 평점/리뷰수 갱신
        content.applyReviewCreated(newRating);
        log.debug("[리뷰] 콘텐츠 평점 갱신(생성): contentId= {}, newReviewCount= {}, newAverageRating= {}",
                content.getId(), content.getReviewCount(), content.getAverageRating());

        //저장
        Review saved = reviewRepository.save(review);

        ReviewDto dto = reviewMapper.toDto(saved);

        log.info("[리뷰] 생성 완료: id= {}", dto.id());
        return dto;
    }

    //리뷰 수정
    @Transactional
    @Override
    public ReviewDto updateReview(UUID reviewId, ReviewUpdateRequest request, UUID currentUserId) {
        log.debug("[리뷰] 수정 시작: reviewId= {}, currentUserId= {}", reviewId, currentUserId);

        //리뷰 조회
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> {
                    log.error("[리뷰] 수정 실패 : 해당 리뷰를 찾을 수 없습니다. reviewId= {}, currentUserId= {}",
                            reviewId, currentUserId);
                    return ReviewNotFoundException.withId(reviewId);
                });

        //작성자 검증
        UUID authorId = review.getUser().getId();
        if (!authorId.equals(currentUserId)) {
            log.error("[리뷰] 수정 권한 없음: reviewId= {}, authorId= {}, currentUserId= {}",
                    reviewId, authorId, currentUserId);
            throw ReviewAccessDeniedException.withId(reviewId);
        }

        Content content = review.getContent();

        int oldRating = review.getRating();
        int newRating = request.rating();

        review.updateReview(request.text(), request.rating());

        content.applyReviewUpdated(oldRating, newRating);
        log.debug("[리뷰] 콘텐츠 평점 갱신(수정): contentId={}, oldRating={}, newRating={}, newAverageRating={}",
                content.getId(), oldRating, newRating, content.getAverageRating());

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
                                               SortDirection sortDirection, ReviewSortBy sortBy) {
        log.debug("[리뷰] 목록 조회 서비스 호출: " +
                        "contentId= {}, cursor= {}, idAfter= {}, limit= {}, sortDirection= {}, sortBy= {}",
                contentId, cursor, idAfter, limit, sortDirection, sortBy);

        //파라미터 보정
        if(limit <= 0 || limit > 50) {
            limit = 10; //기본 페이지 크기(10개 가져옴)
        }

        String sortByValue = sortBy.getValue();  // "createdAt" 또는 "rating"

        //커서 해석 (cursor가 메인)
        UUID effectiveIdAfter = null;
        if (cursor != null && !cursor.isBlank()) {
            try {
                effectiveIdAfter = UUID.fromString(cursor);
            } catch (IllegalArgumentException e) {
                log.error("[리뷰] 잘못된 cursor 형식: cursor= {}", cursor);
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
                sortDirection, sortByValue
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
                sortByValue,
                sortDirection
        );

        log.info("[리뷰] 목록 조회 완료: contentId= {}, 반환개수= {}, hasNext= {}",
                contentId, data.size(), reviews.hasNext());

        return response;
    }

    @Transactional
    @Override
    public void deleteReview(UUID reviewId, UUID currentUserId) {
        String reason = "USER_REQUEST";

        //시작 로그
        log.debug("[리뷰] 삭제 시작: reviewId= {}, currentUserId= {}, reason= {}",
                reviewId, currentUserId, reason);

        //리뷰 조회 + 존재하지 않을 때 로그 + 예외
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> {
                    log.error("[리뷰] 삭제 실패 : 해당 리뷰가 없습니다: reviewId= {}, currentUserId= {}",
                            reviewId, currentUserId);
                    return ReviewNotFoundException.withId(reviewId);
                });

        //작성자 검증
        UUID authorId = review.getUser().getId();
        if (!authorId.equals(currentUserId)) {
            log.error("[리뷰] 삭제 권한 없음: reviewId= {}, authorId= {}, currentUserId= {}",
                    reviewId, authorId, currentUserId);
            throw ReviewAccessDeniedException.withId(reviewId);
        }

        Content content = review.getContent();

        //현재 리뷰의 rating
        int deletedRating = review.getRating();

        //삭제 대상 스냅샷 로그 (사고 났을 때 복원용 핵심 정보)
        log.debug("[리뷰] 삭제 대상 정보: reviewId= {}, contentId= {}, authorId= {}, rating= {}, createdAt= {}",
                review.getId(), review.getContent().getId(), authorId,
                deletedRating, review.getCreatedAt());

        content.applyReviewDeleted(deletedRating);

        log.debug("[리뷰] 콘텐츠 평점 갱신: contentId= {}, newReviewCount= {}, newAverageRating= {}",
                content.getId(), content.getReviewCount(), content.getAverageRating());

        //하드 딜리트 수행
        reviewRepository.delete(review);

        //최종 성공 로그
        log.info("[리뷰] 삭제 완료(하드 딜리트): reviewId= {}, currentUserId= {}, reason= {}",
                reviewId, currentUserId, reason);
    }
}
