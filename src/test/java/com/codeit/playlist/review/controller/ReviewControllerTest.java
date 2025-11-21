package com.codeit.playlist.review.controller;

import com.codeit.playlist.domain.review.controller.ReviewController;
import com.codeit.playlist.domain.review.dto.data.ReviewDto;
import com.codeit.playlist.domain.review.dto.request.ReviewCreateRequest;
import com.codeit.playlist.domain.review.service.ReviewService;
import com.codeit.playlist.domain.security.PlaylistUserDetails;
import com.codeit.playlist.domain.user.dto.data.UserDto;
import com.codeit.playlist.global.config.JpaConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReviewController.class,
        excludeFilters = {
                @ComponentScan.Filter(
                        type = FilterType.ASSIGNABLE_TYPE,
                        classes = com.codeit.playlist.domain.security.jwt.JwtAuthenticationFilter.class
                )
        })
@AutoConfigureMockMvc
@ImportAutoConfiguration(exclude = JpaConfig.class)
public class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReviewService reviewService;

    //@AuthenticationPrincipal PlaylistUserDetails 로 들어갈 인증 객체 생성
    private Authentication createAuthentication(UUID userId) {
        PlaylistUserDetails userDetails = BDDMockito.mock(PlaylistUserDetails.class);
        UserDto userDto = BDDMockito.mock(UserDto.class);

        given(userDto.id()).willReturn(userId);
        given(userDetails.getUserDto()).willReturn(userDto);

        return new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                Collections.emptyList()
        );
    }

    @Test
    @DisplayName("리뷰 생성 성공 - 올바른 요청과 인증된 사용자일 때 201과 ReviewDto를 반환")
    void createReviewSuccess() throws Exception {
        // given
        UUID reviewerId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();

        ReviewCreateRequest request = new ReviewCreateRequest(
                contentId,
                "아주 좋은 컨텐츠네요",
                5
        );

        ReviewDto responseDto = new ReviewDto(
                UUID.randomUUID(),
                contentId,
                null,     // author(UserSummary)는 여기선 단순화
                "아주 좋은 컨텐츠네요",
                5
        );

        given(reviewService.createReview(any(ReviewCreateRequest.class), eq(reviewerId)))
                .willReturn(responseDto);

        // when & then
        mockMvc.perform(post("/api/reviews")
                        .with(authentication(createAuthentication(reviewerId)))          // 인증 사용자
                        .with(csrf())                           // POST + Security 사용 시 CSRF 필요하면 추가
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(responseDto.id().toString()))
                .andExpect(jsonPath("$.contentId").value(contentId.toString()))
                .andExpect(jsonPath("$.text").value("아주 좋은 컨텐츠네요"))
                .andExpect(jsonPath("$.rating").value(5));

        then(reviewService).should()
                .createReview(any(ReviewCreateRequest.class), eq(reviewerId));
    }

    @Test
    @DisplayName("리뷰 생성 실패 - 잘못된 요청 본문이면 400을 반환")
    void createReviewFailWithBadRequest() throws Exception {
        // given
        UUID reviewerId = UUID.randomUUID();

        String invalidJson = """
                {
                  "text": "",
                  "rating": -1
                }
                """;

        // when & then
        mockMvc.perform(post("/api/reviews")
                        .with(authentication(createAuthentication(reviewerId)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        then(reviewService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("리뷰 생성 실패 - 인증되지 않은 사용자는 401을 반환한다")
    void createReviewFailWithUnauthorized() throws Exception {
        // given
        UUID contentId = UUID.randomUUID();

        ReviewCreateRequest request = new ReviewCreateRequest(
                contentId,
                "텍스트",
                3
        );

        // when & then
        mockMvc.perform(post("/api/reviews")
                        // 인증 정보 intentionally 없음
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        then(reviewService).shouldHaveNoInteractions();
    }
}
