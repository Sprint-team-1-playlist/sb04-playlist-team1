package com.codeit.playlist.notification.controller;

import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.notification.controller.NotificationController;
import com.codeit.playlist.domain.notification.dto.response.CursorResponseNotificationDto;
import com.codeit.playlist.domain.notification.service.NotificationService;
import com.codeit.playlist.domain.security.PlaylistUserDetails;
import com.codeit.playlist.domain.user.dto.data.UserDto;
import com.codeit.playlist.global.config.JpaConfig;
import com.codeit.playlist.global.error.InvalidSortByException;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NotificationController.class,
        excludeFilters = {
                @ComponentScan.Filter(
                        type = FilterType.ASSIGNABLE_TYPE,
                        classes = com.codeit.playlist.domain.security.jwt.JwtAuthenticationFilter.class
                )
        })
@AutoConfigureMockMvc
@ImportAutoConfiguration(exclude = JpaConfig.class)
public class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NotificationService notificationService;

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
    @DisplayName("알림 목록 조회 성공 - 200 OK")
    void getNotificationListSuccess() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        Authentication authentication = createAuthentication(userId);

        CursorResponseNotificationDto responseDto = new CursorResponseNotificationDto(
                List.of(),          // data
                null,               // nextCursor
                null,               // nextIdAfter
                false,              // hasNext
                0L,                 // totalCount
                "createdAt",        // sortBy
                SortDirection.DESCENDING // sortDirection
        );

        given(notificationService.getAllNotifications(
                eq(userId),
                isNull(),
                isNull(),
                eq(10),
                eq(SortDirection.DESCENDING),
                eq("createdAt")
        )).willReturn(responseDto);

        // when & then
        mockMvc.perform(get("/api/notifications")
                        .param("limit", "10")
                        .param("sortDirection", "DESCENDING")
                        .param("sortBy", "createdAt")
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.totalCount").value(0));

        then(notificationService).should().getAllNotifications(
                eq(userId),
                isNull(),
                isNull(),
                eq(10),
                eq(SortDirection.DESCENDING),
                eq("createdAt")
        );
    }

    @Test
    @DisplayName("알림 목록 조회 실패 - 잘못된 파라미터로 400 Bad Request")
    void getNotificationListFailWithBadRequest() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        Authentication authentication = createAuthentication(userId);

        given(notificationService.getAllNotifications(
                eq(userId),
                any(),              // cursor
                any(),              // idAfter
                anyInt(),           // limit
                any(),              // sortDirection
                eq("wrongField")    // sortBy
        )).willThrow(InvalidSortByException.withSortBy("wrongField"));

        // when & then
        mockMvc.perform(get("/api/notifications")
                        .param("limit", "10")
                        .param("sortDirection", "DESCENDING")
                        .param("sortBy", "wrongField")
                        .with(authentication(authentication)))
                .andExpect(status().isBadRequest());

        then(notificationService).should().getAllNotifications(
                eq(userId),
                any(),
                any(),
                anyInt(),
                any(),
                eq("wrongField")
        );

    }

    @Test
    @DisplayName("알림 목록 조회 실패 - 인증 없음 401 Unauthorized")
    void getNotificationListFailWithUnauthorized() throws Exception {
        // given
        // 인증 정보 없음

        // when & then
        mockMvc.perform(get("/api/notifications")
                        .param("limit", "10")
                        .param("sortDirection", "DESCENDING")
                        .param("sortBy", "createdAt"))
                .andExpect(status().isUnauthorized()); // SecurityConfig 설정에 따라 401 기대

        then(notificationService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("알림 목록 조회 실패 - 내부 서버 오류 500 Internal Server Error")
    void getNotificationListFailWithInternalServerError() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        Authentication authentication = createAuthentication(userId);

        given(notificationService.getAllNotifications(
                eq(userId),
                any(),
                any(),
                anyInt(),
                any(),
                anyString()
        )).willThrow(new RuntimeException("테스트 예외"));

        // when & then
        mockMvc.perform(get("/api/notifications")
                        .param("limit", "10")
                        .param("sortDirection", "DESCENDING")
                        .param("sortBy", "createdAt")
                        .with(authentication(authentication)))
                .andExpect(status().isInternalServerError());

        then(notificationService).should().getAllNotifications(
                eq(userId),
                any(),
                any(),
                anyInt(),
                any(),
                anyString()
        );
    }

    @Test
    @DisplayName("알림 읽음 처리 성공 - 본인 요청, 204 No Content")
    void deleteNotificationSuccess() throws Exception {
        // given
        UUID notificationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Authentication authentication = createAuthentication(userId);

        // when & then
        mockMvc.perform(delete("/api/notifications/{notificationId}", notificationId)
                        .with(authentication(authentication))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        // 서비스가 올바른 파라미터로 호출되었는지 검증
        then(notificationService).should()
                .markAsReadAndDeleteNotification(eq(notificationId), eq(userId));
    }

    @Test
    @DisplayName("알림 읽음 처리 실패 - 잘못된 PathVariable(UUID 아님)로 400 Bad Request")
    void deleteNotificationFailWithBadRequest() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        Authentication authentication = createAuthentication(userId);

        // when & then
        mockMvc.perform(delete("/api/notifications/{notificationId}", "not-a-uuid")
                        .with(authentication(authentication))
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        // 서비스는 호출되면 안 됨
        then(notificationService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("알림 읽음 처리 실패 - 인증 정보 없으면 401 Unauthorized")
    void deleteNotificationFailWithUnauthorized() throws Exception {
        // given
        UUID notificationId = UUID.randomUUID();

        // when & then
        mockMvc.perform(delete("/api/notifications/{notificationId}", notificationId)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());

        // 서비스는 호출되면 안 됨
        then(notificationService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("알림 읽음 처리 실패 - 서비스 내부 예외 발생 시 500 Internal Server Error")
    void deleteNotificationFailWithInternalServerError() throws Exception {
        // given
        UUID notificationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Authentication authentication = createAuthentication(userId);

        willThrow(new RuntimeException("DB error"))
                .given(notificationService)
                .markAsReadAndDeleteNotification(eq(notificationId), eq(userId));

        // when & then
        mockMvc.perform(delete("/api/notifications/{notificationId}", notificationId)
                        .with(authentication(authentication))
                        .with(csrf()))
                .andExpect(status().isInternalServerError());

        then(notificationService).should()
                .markAsReadAndDeleteNotification(eq(notificationId), eq(userId));
    }
}
