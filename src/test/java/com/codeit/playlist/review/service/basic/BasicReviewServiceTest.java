package com.codeit.playlist.review.service.basic;

import com.codeit.playlist.domain.content.entity.Content;
import com.codeit.playlist.domain.content.exception.ContentNotFoundException;
import com.codeit.playlist.domain.content.repository.ContentRepository;
import com.codeit.playlist.domain.review.dto.data.ReviewDto;
import com.codeit.playlist.domain.review.dto.request.ReviewCreateRequest;
import com.codeit.playlist.domain.review.entity.Review;
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

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
}
