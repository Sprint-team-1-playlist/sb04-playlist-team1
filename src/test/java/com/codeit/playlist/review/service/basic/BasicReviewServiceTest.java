package com.codeit.playlist.review.service.basic;

import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.content.entity.Content;
import com.codeit.playlist.domain.content.exception.ContentNotFoundException;
import com.codeit.playlist.domain.content.repository.ContentRepository;
import com.codeit.playlist.domain.review.dto.data.ReviewSortBy;
import com.codeit.playlist.domain.review.dto.data.ReviewDto;
import com.codeit.playlist.domain.review.dto.request.ReviewCreateRequest;
import com.codeit.playlist.domain.review.dto.request.ReviewUpdateRequest;
import com.codeit.playlist.domain.review.dto.response.CursorResponseReviewDto;
import com.codeit.playlist.domain.review.entity.Review;
import com.codeit.playlist.domain.review.exception.ReviewAccessDeniedException;
import com.codeit.playlist.domain.review.exception.ReviewNotFoundException;
import com.codeit.playlist.domain.review.mapper.ReviewMapper;
import com.codeit.playlist.domain.review.repository.ReviewRepository;
import com.codeit.playlist.domain.review.service.basic.BasicReviewService;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.exception.UserNotFoundException;
import com.codeit.playlist.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
public class BasicReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewMapper reviewMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ContentRepository contentRepository;

    @InjectMocks
    private BasicReviewService basicReviewService;

    @Test
    @DisplayName("리뷰 생성 성공 - 정상 요청이면 리뷰가 생성되고 DTO 반환")
    void createReviewSuccess() {
        //given
        UUID reviewerId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();

        ReviewCreateRequest request =
                new ReviewCreateRequest(contentId, "좋은 컨텐츠네요", 5);

        User reviewer = mock(User.class);
        Content content = mock(Content.class);
        Review reviewEntity = mock(Review.class);
        Review savedReview = mock(Review.class);

        ReviewDto expectedDto = new ReviewDto(
                UUID.randomUUID(),
                contentId,
                // author는 실제 UserSummary 타입으로 맞춰서 사용
                // 여기서는 단순 mock/fixture로 가정
                null,
                "좋은 컨텐츠네요",
                5
        );

        given(userRepository.findById(reviewerId)).willReturn(Optional.of(reviewer));
        given(contentRepository.findById(contentId)).willReturn(Optional.of(content));
        given(reviewMapper.toEntity(request, content, reviewer)).willReturn(reviewEntity);
        given(reviewRepository.save(reviewEntity)).willReturn(savedReview);
        given(reviewMapper.toDto(savedReview)).willReturn(expectedDto);

        //when
        ReviewDto result = basicReviewService.createReview(request, reviewerId);

        //then
        assertThat(result).isSameAs(expectedDto);

        then(userRepository).should().findById(reviewerId);
        then(contentRepository).should().findById(contentId);
        then(reviewMapper).should().toEntity(request, content, reviewer);
        then(reviewRepository).should().save(reviewEntity);
        then(reviewMapper).should().toDto(savedReview);
    }

    @Test
    @DisplayName("createReview 실패(400) - 존재하지 않는 콘텐츠 ID면 ContentNotFoundException 발생")
    void createReviewFail_whenContentNotFound_shouldThrowContentNotFoundException() {
        // given
        UUID reviewerId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();

        ReviewCreateRequest request =
                new ReviewCreateRequest(contentId, "텍스트", 3);

        User reviewer = mock(User.class);

        given(userRepository.findById(reviewerId)).willReturn(Optional.of(reviewer));
        given(contentRepository.findById(contentId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> basicReviewService.createReview(request, reviewerId))
                .isInstanceOf(ContentNotFoundException.class);

        then(userRepository).should().findById(reviewerId);
        then(contentRepository).should().findById(contentId);
        // 콘텐츠 찾기에서 이미 예외이므로 이하 호출되지 않음
        then(reviewMapper).shouldHaveNoInteractions();
        then(reviewRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("createReview 실패(401) - 존재하지 않는 사용자 ID면 UserNotFoundException 발생")
    void createReviewFail_whenUserNotFound_shouldThrowUserNotFoundException() {
        // given
        UUID reviewerId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();

        ReviewCreateRequest request =
                new ReviewCreateRequest(contentId, "텍스트", 3);

        given(userRepository.findById(reviewerId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> basicReviewService.createReview(request, reviewerId))
                .isInstanceOf(UserNotFoundException.class);

        then(userRepository).should().findById(reviewerId);
        // 유저 조회에서 바로 예외가 나기 때문에 나머지는 호출 X
        then(contentRepository).shouldHaveNoInteractions();
        then(reviewMapper).shouldHaveNoInteractions();
        then(reviewRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("createReview 실패(500) - 저장 중 예상치 못한 예외가 발생하면 그대로 전파된다")
    void createReviewFail_whenUnexpectedErrorOnSave_shouldThrowRuntimeException() {
        // given
        UUID reviewerId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();

        ReviewCreateRequest request =
                new ReviewCreateRequest(contentId, "텍스트", 3);

        User reviewer = mock(User.class);
        Content content = mock(Content.class);
        Review reviewEntity = mock(Review.class);

        given(userRepository.findById(reviewerId)).willReturn(Optional.of(reviewer));
        given(contentRepository.findById(contentId)).willReturn(Optional.of(content));
        given(reviewMapper.toEntity(request, content, reviewer)).willReturn(reviewEntity);
        given(reviewRepository.save(reviewEntity))
                .willThrow(new RuntimeException("DB error"));

        // when & then
        assertThatThrownBy(() -> basicReviewService.createReview(request, reviewerId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DB error");

        then(userRepository).should().findById(reviewerId);
        then(contentRepository).should().findById(contentId);
        then(reviewMapper).should().toEntity(request, content, reviewer);
        then(reviewRepository).should().save(reviewEntity);
        then(reviewMapper).should(never()).toDto(any());
    }

    @Test
    @DisplayName("updateReview 성공 - 본인 리뷰를 정상적으로 수정하고 DTO를 반환")
    void updateReviewSuccess() {
        // given
        UUID reviewId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        ReviewUpdateRequest request = new ReviewUpdateRequest("수정된 내용", 4);

        User reviewer = mock(User.class);
        given(reviewer.getId()).willReturn(currentUserId);

        Content content = mock(Content.class);

        Review review = mock(Review.class);
        given(review.getUser()).willReturn(reviewer);
        given(review.getContent()).willReturn(content);
        given(review.getRating()).willReturn(3);

        Review savedReview = mock(Review.class);

        ReviewDto expectedDto = new ReviewDto(
                reviewId,
                UUID.randomUUID(),
                null,
                "수정된 내용",
                4
        );

        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));
        given(reviewRepository.save(review)).willReturn(savedReview);
        given(reviewMapper.toDto(savedReview)).willReturn(expectedDto);

        // when
        ReviewDto result = basicReviewService.updateReview(reviewId, request, currentUserId);

        // then
        assertThat(result).isEqualTo(expectedDto);

        then(reviewRepository).should().findById(reviewId);
        then(review).should().updateReview("수정된 내용", 4);
        then(content).should().applyReviewUpdated(3, 4);
        then(reviewRepository).should().save(review);
        then(reviewMapper).should().toDto(savedReview);
    }

    @Test
    @DisplayName("updateReview 실패 - 리뷰가 존재하지 않으면 ReviewNotFoundException 발생")
    void updateReviewFailWithNotFound() {
        // given
        UUID reviewId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        ReviewUpdateRequest request = new ReviewUpdateRequest("내용", 5);

        given(reviewRepository.findById(reviewId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> basicReviewService.updateReview(reviewId, request, currentUserId))
                .isInstanceOf(ReviewNotFoundException.class);

        then(reviewRepository).should().findById(reviewId);
        then(reviewRepository).shouldHaveNoMoreInteractions();
        then(reviewMapper).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("updateReview 실패 - 리뷰 작성자의 User 정보가 없으면 UserNotFoundException 발생")
    void updateReviewFail_UnauthorizedWithUserNotFound() {
        // given
        UUID reviewId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        ReviewUpdateRequest request = new ReviewUpdateRequest("내용", 5);

        // 리뷰가 있고, reviewer.getId() 호출 시 null 또는 예외
        User reviewer = mock(User.class);
        given(reviewer.getId()).willThrow(new UserNotFoundException());

        Review review = mock(Review.class);
        given(review.getUser()).willReturn(reviewer);

        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

        // when & then
        assertThatThrownBy(() -> basicReviewService.updateReview(reviewId, request, currentUserId))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("updateReview 실패 - 리뷰 작성자가 아니면 ReviewAccessDeniedException 발생")
    void updateReviewFailWithForbidden() {
        // given
        UUID reviewId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        UUID writerId = UUID.randomUUID(); // 다른 사람

        ReviewUpdateRequest request = new ReviewUpdateRequest("내용", 5);

        User reviewer = mock(User.class);
        given(reviewer.getId()).willReturn(writerId);

        Review review = mock(Review.class);
        given(review.getUser()).willReturn(reviewer);

        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

        // when & then
        assertThatThrownBy(() -> basicReviewService.updateReview(reviewId, request, currentUserId))
                .isInstanceOf(ReviewAccessDeniedException.class);
    }

    @Test
    @DisplayName("updateReview 실패 - 저장 과정에서 예상치 못한 예외 발생 시 RuntimeException 발생")
    void updateReviewFailWithInternalServerError() {
        // given
        UUID reviewId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        ReviewUpdateRequest request = new ReviewUpdateRequest("내용", 5);

        User reviewer = mock(User.class);
        given(reviewer.getId()).willReturn(currentUserId);

        Review review = mock(Review.class);
        Content content = mock(Content.class);

        given(review.getUser()).willReturn(reviewer);
        given(review.getContent()).willReturn(content);
        given(review.getRating()).willReturn(3);

        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));
        // save에서 갑자기 터지도록 설정
        given(reviewRepository.save(any())).willThrow(new RuntimeException("DB Failure"));

        // when & then
        assertThatThrownBy(() -> basicReviewService.updateReview(reviewId, request, currentUserId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DB Failure");

        then(reviewRepository).should().findById(reviewId);
        then(review).should().updateReview("내용", 5);
        then(content).should().applyReviewUpdated(3, 5);
        then(reviewRepository).should().save(review);
        then(reviewMapper).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("리뷰 목록 조회 성공 - 커서 기반 페이징과 totalCount, nextCursor가 올바르게 계산된다")
    void findReviewsSuccess() {
        // given
        UUID contentId = UUID.randomUUID();

        UUID previousCursor = UUID.randomUUID();
        String cursor = previousCursor.toString();
        UUID idAfter = null;

        int limit = 2;
        SortDirection sortDirection = SortDirection.DESCENDING;
        ReviewSortBy sortBy = ReviewSortBy.createdAt;

        // 리뷰에 들어갈 user / content
        User user = mock(User.class);
        Content content = mock(Content.class);

        Review r1 = createReview(user, content, 4, "리뷰1");
        Review r2 = createReview(user, content, 5, "리뷰2");

        UUID review1Id = r1.getId();
        UUID review2Id = r2.getId();

        Slice<Review> slice = new SliceImpl<>(
                List.of(r1, r2),
                PageRequest.of(0, limit),
                true   // hasNext = true → nextCursor 계산 대상
        );

        given(reviewRepository.findReviews(
                eq(contentId),
                eq(cursor),
                any(),
                eq(limit),
                eq(sortDirection),
                eq("createdAt")
        )).willReturn(slice);

        ReviewDto dto1 = mock(ReviewDto.class);
        ReviewDto dto2 = mock(ReviewDto.class);
        given(reviewMapper.toDto(r1)).willReturn(dto1);
        given(reviewMapper.toDto(r2)).willReturn(dto2);

        given(reviewRepository.countByContent_Id(contentId))
                .willReturn(50L);

        // when
        CursorResponseReviewDto response = basicReviewService.findReviews(
                contentId, cursor,
                idAfter, limit,
                sortDirection, sortBy
        );

        // then
        assertThat(response).isNotNull();
        assertThat(response.data()).containsExactly(dto1, dto2);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.totalCount()).isEqualTo(50L);
        assertThat(response.sortBy()).isEqualTo("createdAt");
        assertThat(response.sortDirection()).isEqualTo(SortDirection.DESCENDING);

        assertThat(response.nextIdAfter()).isEqualTo(review2Id);
        assertThat(response.nextCursor()).isEqualTo(review2Id.toString());

        then(reviewRepository).should().findReviews(
                eq(contentId),
                eq(cursor),
                any(),
                eq(limit),
                eq(sortDirection),
                eq("createdAt")
        );
        then(reviewRepository).should().countByContent_Id(contentId);
    }

    @Test
    @DisplayName("findReviews 실패 - 잘못된 cursor 문자열이 넘어와도 예외 없이 첫 페이지로 처리한다")
    void findReviewsInvalidCursorDoesNotThrow() {
        // given
        UUID contentId = UUID.randomUUID();
        String invalidCursor = "not-a-uuid";
        UUID idAfter = null;
        int limit = 3;

        Slice<Review> emptySlice = new SliceImpl<>(
                List.of(),
                PageRequest.of(0, limit),
                false
        );

        given(reviewRepository.findReviews(
                eq(contentId),
                eq(invalidCursor),
                any(),
                eq(limit),
                eq(SortDirection.ASCENDING),
                eq("createdAt")
        )).willReturn(emptySlice);

        given(reviewRepository.countByContent_Id(contentId))
                .willReturn(0L);

        // when
        CursorResponseReviewDto response = basicReviewService.findReviews(
                contentId,
                invalidCursor,
                idAfter,
                limit,
                SortDirection.ASCENDING,
                ReviewSortBy.createdAt
        );

        // then
        assertThat(response.data()).isEmpty();
        assertThat(response.hasNext()).isFalse();
        assertThat(response.nextCursor()).isNull();
        assertThat(response.nextIdAfter()).isNull();
        assertThat(response.totalCount()).isEqualTo(0L);

        then(reviewRepository).should().findReviews(
                eq(contentId),
                eq(invalidCursor),
                any(),
                eq(limit),
                eq(SortDirection.ASCENDING),
                eq("createdAt")
        );
        then(reviewRepository).should().countByContent_Id(contentId);
    }

    private Review createReview(User user, Content content, int rating, String text) {
        try {
            //protected 생성자 열기
            Constructor<Review> constructor = Review.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            Review review = constructor.newInstance();

            // 2) Review 필드 세팅
            ReflectionTestUtils.setField(review, "user", user);
            ReflectionTestUtils.setField(review, "content", content);
            ReflectionTestUtils.setField(review, "rating", rating);
            ReflectionTestUtils.setField(review, "text", text);

            //BaseEntity / BaseUpdatableEntity 필드 세팅
            ReflectionTestUtils.setField(review, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(review, "createdAt", LocalDateTime.now());
            ReflectionTestUtils.setField(review, "updatedAt", LocalDateTime.now());

            return review;
        } catch (Exception e) {
            throw new RuntimeException("리뷰 테스트 객체 생성 실패", e);
        }
    }

    @Test
    @DisplayName("리뷰 삭제 성공 - 작성자가 자신의 리뷰를 삭제하면 콘텐츠 평점 반영 후 하드 딜리트된다")
    void deleteReviewSuccess() {
        // given
        UUID reviewId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();

        Review review = mock(Review.class);
        User author = mock(User.class);
        Content content = mock(Content.class);

        int rating = 5;

        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));
        given(review.getUser()).willReturn(author);
        given(author.getId()).willReturn(currentUserId);
        given(review.getContent()).willReturn(content);
        given(review.getRating()).willReturn(rating);

        // when
        basicReviewService.deleteReview(reviewId, currentUserId);

        // then
        then(reviewRepository).should().findById(reviewId);
        then(review).should().getUser();
        then(content).should().applyReviewDeleted(rating);  // 평점 반영
        then(reviewRepository).should().delete(review);     // 하드 딜리트
    }

    @Test
    @DisplayName("리뷰 삭제 실패 - 존재하지 않는 리뷰 ID면 ReviewNotFoundException 발생")
    void deleteReviewFailWhenReviewNotFound() {
        // given
        UUID reviewId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();

        given(reviewRepository.findById(reviewId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> basicReviewService.deleteReview(reviewId, currentUserId))
                .isInstanceOf(ReviewNotFoundException.class);

        then(reviewRepository).should().findById(reviewId);
        // 삭제는 호출되면 안 됨
        then(reviewRepository).should(never()).delete(any(Review.class));
    }

    @Test
    @DisplayName("리뷰 삭제 실패 - 현재 사용자가 작성자가 아니면 ReviewAccessDeniedException 발생")
    void deleteReviewFailWhenAccessDenied() {
        // given
        UUID reviewId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();          // 요청 유저
        UUID authorId = UUID.randomUUID();               // 다른 사람

        Review review = mock(Review.class);
        User author = mock(User.class);

        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));
        given(review.getUser()).willReturn(author);
        given(author.getId()).willReturn(authorId);

        // when & then
        assertThatThrownBy(() -> basicReviewService.deleteReview(reviewId, currentUserId))
                .isInstanceOf(ReviewAccessDeniedException.class);

        then(reviewRepository).should().findById(reviewId);
        // 작성자 검증 이후 실패하므로, 콘텐츠/삭제 로직은 호출되면 안 됨
        then(review).should(never()).getContent();
        then(reviewRepository).should(never()).delete(any(Review.class));
    }
}
