package com.codeit.playlist.watching.controller;

import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.watching.controller.WatchingController;
import com.codeit.playlist.domain.watching.dto.data.SortBy;
import com.codeit.playlist.domain.watching.dto.request.WatchingSessionRequest;
import com.codeit.playlist.domain.watching.service.WatchingService;
import com.codeit.playlist.watching.fixture.WatchingSessionFixtures;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WatchingControllerTest {
    @InjectMocks
    private WatchingController watchingController;

    @Mock
    private WatchingService watchingService;

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void beforeAll() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void afterAll() {
        if (validatorFactory != null) validatorFactory.close();
    }

    @Test
    @DisplayName("정상 요청이면 검증 성공")
    void validRequestSuccess() {
        // given
        WatchingSessionRequest request = WatchingSessionFixtures.watchingSessionRequest();

        // when
        Set<ConstraintViolation<WatchingSessionRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("watcherNameLike 가 빈 값이면 실패")
    void faliIfWatcherNameLikeIsBlank() {
        // given
        WatchingSessionRequest request = new WatchingSessionRequest(
                null, // invalid
                "cursor",
                WatchingSessionFixtures.FIXED_ID,
                10,
                SortDirection.ASCENDING,
                SortBy.createdAt
        );

        // when
        Set<ConstraintViolation<WatchingSessionRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("watcherNameLike")
                        && v.getMessage().equals("사용자 이름 필수")
        );
    }

    @Test
    @DisplayName("limit이 1 미만이면 실패")
    void failIfLimitIsLessThanOne() {
        WatchingSessionRequest request = new WatchingSessionRequest(
                "test",
                "cursor",
                WatchingSessionFixtures.FIXED_ID,
                0, // invalid
                SortDirection.ASCENDING,
                SortBy.createdAt
        );

        Set<ConstraintViolation<WatchingSessionRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("limit")
                        && v.getMessage().equals("limit은 최소 1 이상")
        );
    }

    @Test
    @DisplayName("sortDirection null 이면 실패")
    void failIfSortDirectionIsNull() {
        WatchingSessionRequest request = new WatchingSessionRequest(
                "test",
                "cursor",
                WatchingSessionFixtures.FIXED_ID,
                10,
                null, // invalid
                SortBy.createdAt
        );

        Set<ConstraintViolation<WatchingSessionRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("sortDirection")
                        && v.getMessage().equals("정렬 방향 필수")
        );
    }

    @Test
    @DisplayName("sortBy null 이면 실패")
    void failIfSortByIsNull() {
        WatchingSessionRequest request = new WatchingSessionRequest(
                "test",
                "cursor",
                WatchingSessionFixtures.FIXED_ID,
                10,
                SortDirection.ASCENDING,
                null // invalid
        );

        Set<ConstraintViolation<WatchingSessionRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("sortBy")
                        && v.getMessage().equals("졍렬 기준 필수")
        );
    }

    @Test
    @DisplayName("getWatchingSessions 호출 시 watchingService.getWatchingSessions()이 호출됨")
    void getWatchingSessionsShouldCallService() {
        // given
        UUID contentId = WatchingSessionFixtures.FIXED_ID;

        // when
        watchingController.getWatchingSessions(contentId, WatchingSessionFixtures.watchingSessionRequest());

        // then
        verify(watchingService, times(1))
                .getWatchingSessions(contentId, WatchingSessionFixtures.watchingSessionRequest());
    }
}