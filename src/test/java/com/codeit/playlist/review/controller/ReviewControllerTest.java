package com.codeit.playlist.review.controller;

import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.review.controller.ReviewController;
import com.codeit.playlist.domain.review.dto.data.ReviewDto;
import com.codeit.playlist.domain.review.dto.request.ReviewCreateRequest;
import com.codeit.playlist.domain.review.dto.request.ReviewUpdateRequest;
import com.codeit.playlist.domain.review.dto.response.CursorResponseReviewDto;
import com.codeit.playlist.domain.review.exception.ReviewAccessDeniedException;
import com.codeit.playlist.domain.review.service.ReviewService;
import com.codeit.playlist.domain.security.PlaylistUserDetails;
import com.codeit.playlist.domain.user.dto.data.UserDto;
import com.codeit.playlist.global.config.JpaConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

    @MockitoBean
    private ReviewService reviewService;

    //테스트용 SecurityConfig
    @TestConfiguration
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
            return http
                    // CSRF는 테스트에서 귀찮으니 끔 (네가 .with(csrf()) 넣어도 상관 없음)
                    .csrf(AbstractHttpConfigurer::disable)

                    // 로그인 페이지/리다이렉트 방지 (302 원인 제거)
                    .formLogin(AbstractHttpConfigurer::disable)
                    .httpBasic(AbstractHttpConfigurer::disable)
                    .logout(AbstractHttpConfigurer::disable)
                    .oauth2Login(AbstractHttpConfigurer::disable)

                    // 아무 요청이나 "인증 필요"만 걸어둔다
                    .authorizeHttpRequests(auth -> auth
                            .anyRequest().authenticated()
                    )

                    // 인증 실패 시 302 대신 401 반환하도록 명시
                    .exceptionHandling(ex -> ex
                            .authenticationEntryPoint((req, res, e) ->
                                    res.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                    )

                    .build();
        }
    }

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

    @Test
    @DisplayName("리뷰 수정 성공 - 본인 리뷰를 올바르게 수정하면 200과 수정된 ReviewDto를 반환")
    void updateReviewSuccess() throws Exception {
        // given
        UUID reviewId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();

        ReviewUpdateRequest request = new ReviewUpdateRequest(
                "수정된 리뷰 내용",
                4
        );

        ReviewDto responseDto = new ReviewDto(
                reviewId,
                UUID.randomUUID(),   // contentId
                null,                // author(UserSummary) - 여기선 단순화
                "수정된 리뷰 내용",
                4
        );

        given(reviewService.updateReview(eq(reviewId), any(ReviewUpdateRequest.class), eq(currentUserId)))
                .willReturn(responseDto);

        // when & then
        mockMvc.perform(patch("/api/reviews/{reviewId}", reviewId)
                        .with(authentication(createAuthentication(currentUserId)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reviewId.toString()))
                .andExpect(jsonPath("$.text").value("수정된 리뷰 내용"))
                .andExpect(jsonPath("$.rating").value(4));

        then(reviewService).should()
                .updateReview(eq(reviewId), any(ReviewUpdateRequest.class), eq(currentUserId));
    }

    @Test
    @DisplayName("리뷰 수정 실패 - 유효하지 않은 요청 본문이면 400을 반환")
    void updateReviewFailWithBadRequest() throws Exception {
        // given
        UUID reviewId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();

        // text가 빈 문자열, rating이 음수 등으로 Bean Validation 위반 유도
        String invalidJson = """
                {
                  "text": "",
                  "rating": -1
                }
                """;

        // when & then
        mockMvc.perform(patch("/api/reviews/{reviewId}", reviewId)
                        .with(authentication(createAuthentication(currentUserId)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        // 검증 단계에서 막혀야 하므로 서비스는 호출되면 안 됨
        then(reviewService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("리뷰 수정 실패 - 인증되지 않은 사용자는 401을 반환")
    void updateReviewFailWithUnauthorized() throws Exception {
        // given
        UUID reviewId = UUID.randomUUID();

        ReviewUpdateRequest request = new ReviewUpdateRequest(
                "수정 내용",
                3
        );

        // when & then
        mockMvc.perform(patch("/api/reviews/{reviewId}", reviewId)
                        // 인증 정보 intentionally 없음
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        then(reviewService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("리뷰 수정 실패 - 다른 사용자의 리뷰를 수정하려 하면 403을 반환")
    void updateReviewFailWithForbidden() throws Exception {
        // given
        UUID reviewId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();

        ReviewUpdateRequest request = new ReviewUpdateRequest(
                "수정 내용",
                3
        );

        // 서비스 계층에서 권한 예외 발생 시나리오
        given(reviewService.updateReview(eq(reviewId), any(ReviewUpdateRequest.class), eq(currentUserId)))
                .willThrow(ReviewAccessDeniedException.withId(reviewId));

        // when & then
        mockMvc.perform(patch("/api/reviews/{reviewId}", reviewId)
                        .with(authentication(createAuthentication(currentUserId)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        then(reviewService).should()
                .updateReview(eq(reviewId), any(ReviewUpdateRequest.class), eq(currentUserId));
    }

    @Test
    @DisplayName("리뷰 수정 실패 - 서비스 내부에서 예기치 못한 예외가 발생하면 500을 반환")
    void updateReviewFailWithInternalServerError() throws Exception {
        // given
        UUID reviewId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();

        ReviewUpdateRequest request = new ReviewUpdateRequest(
                "수정 내용",
                3
        );

        given(reviewService.updateReview(eq(reviewId), any(ReviewUpdateRequest.class), eq(currentUserId)))
                .willThrow(new RuntimeException("테스트용 예외"));

        // when & then
        mockMvc.perform(patch("/api/reviews/{reviewId}", reviewId)
                        .with(authentication(createAuthentication(currentUserId)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());

        then(reviewService).should()
                .updateReview(eq(reviewId), any(ReviewUpdateRequest.class), eq(currentUserId));
    }

    @Test
    @WithMockUser(username = "tester", roles = "USER")
    @DisplayName("리뷰 목록 조회 성공 - 200 OK")
    void getReviewListSuccess() throws Exception {
        // given
        UUID contentId = UUID.randomUUID();
        int limit = 10;

        CursorResponseReviewDto responseDto = new CursorResponseReviewDto(
                Collections.emptyList(),
                "NEXT_CURSOR",
                null,
                false,
                0L,
                "createdAt",
                SortDirection.DESCENDING
        );

        given(reviewService.findReviews(
                eq(contentId),
                isNull(),
                isNull(),
                eq(limit),
                eq(SortDirection.DESCENDING),
                eq("createdAt")
        )).willReturn(responseDto);

        // when & then
        mockMvc.perform(get("/api/reviews")
                        .param("contentId", contentId.toString())
                        .param("limit", String.valueOf(limit))
                        .param("sortDirection", "DESCENDING")
                        .param("sortBy", "createdAt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextCursor").value("NEXT_CURSOR"))
                .andExpect(jsonPath("$.sortBy").value("createdAt"))
                .andExpect(jsonPath("$.sortDirection").value("DESCENDING"));

        then(reviewService).should().findReviews(
                eq(contentId),
                isNull(),
                isNull(),
                eq(limit),
                eq(SortDirection.DESCENDING),
                eq("createdAt")
        );
    }

    @Test
    @DisplayName("리뷰 목록 조회 실패 - 필수 파라미터(contentId) 누락 시 400 Bad Request")
    @WithMockUser(username = "tester", roles = "USER")
    void getReviewListFailBadRequest() throws Exception {
        // given
        int limit = 10;

        // when & then
        mockMvc.perform(get("/api/reviews")
                        .param("limit", String.valueOf(limit)))
                .andExpect(status().isBadRequest());

        then(reviewService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("리뷰 목록 조회 실패 - 인증되지 않은 사용자는 401 (SecurityConfig에 따라 401/403 조정 필요)")
    @WithAnonymousUser
    void getReviewListFailUnauthorized() throws Exception {
        // given
        UUID contentId = UUID.randomUUID();

        // when & then
        mockMvc.perform(get("/api/reviews")
                        .param("contentId", contentId.toString())
                        .param("limit", "10"))
                .andExpect(status().isUnauthorized());

        then(reviewService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("리뷰 목록 조회 실패 - 서비스에서 예외 발생 시 500 Internal Server Error")
    @WithMockUser(username = "tester", roles = "USER")
    void getReviewListFailServerError() throws Exception {
        // given
        UUID contentId = UUID.randomUUID();
        int limit = 10;

        given(reviewService.findReviews(
                any(UUID.class),
                any(),
                any(),
                anyInt(),
                any(SortDirection.class),
                eq("createdAt")
        )).willThrow(new RuntimeException("테스트용 예외"));

        // when & then
        mockMvc.perform(get("/api/reviews")
                        .param("contentId", contentId.toString())
                        .param("limit", String.valueOf(limit)))
                .andExpect(status().isInternalServerError());

        then(reviewService).should().findReviews(
                any(UUID.class),
                any(),
                any(),
                anyInt(),
                any(SortDirection.class),
                eq("createdAt")
        );
    }

    @Test
    @DisplayName("리뷰 삭제 성공 - 204 No Content")
    void deleteReviewSuccess() throws Exception {
        // given
        UUID reviewId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();

        Authentication authentication = createAuthentication(currentUserId);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        willDoNothing().given(reviewService).deleteReview(reviewId, currentUserId);

        // when & then
        mockMvc.perform(delete("/api/reviews/{reviewId}", reviewId)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        then(reviewService).should().deleteReview(reviewId, currentUserId);

        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("리뷰 삭제 실패 - 인증되지 않은 사용자는 401")
    void deleteReviewFailUnauthorized() throws Exception {
        // given
        SecurityContextHolder.clearContext();
        UUID reviewId = UUID.randomUUID();

        // when & then
        mockMvc.perform(delete("/api/reviews/{reviewId}", reviewId)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());

        then(reviewService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("리뷰 삭제 실패 - 서비스 예외 발생 시 500 Internal Server Error")
    void deleteReviewFailServerError() throws Exception {
        // given
        UUID reviewId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();

        Authentication authentication = createAuthentication(currentUserId);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        willThrow(new RuntimeException("테스트 예외"))
                .given(reviewService).deleteReview(reviewId, currentUserId);

        // when & then
        mockMvc.perform(delete("/api/reviews/{reviewId}", reviewId)
                        .with(csrf()))
                .andExpect(status().isInternalServerError());

        then(reviewService).should().deleteReview(reviewId, currentUserId);

        SecurityContextHolder.clearContext();
    }
}
