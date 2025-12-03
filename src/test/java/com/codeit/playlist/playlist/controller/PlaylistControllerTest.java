package com.codeit.playlist.playlist.controller;

import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.playlist.controller.PlaylistController;
import com.codeit.playlist.domain.playlist.dto.data.PlaylistDto;
import com.codeit.playlist.domain.playlist.dto.data.PlaylistSortBy;
import com.codeit.playlist.domain.playlist.dto.request.PlaylistCreateRequest;
import com.codeit.playlist.domain.playlist.dto.request.PlaylistUpdateRequest;
import com.codeit.playlist.domain.playlist.dto.response.CursorResponsePlaylistDto;
import com.codeit.playlist.domain.playlist.service.PlaylistContentService;
import com.codeit.playlist.domain.playlist.service.PlaylistService;
import com.codeit.playlist.domain.playlist.service.PlaylistSubscriptionService;
import com.codeit.playlist.domain.security.PlaylistUserDetails;
import com.codeit.playlist.domain.user.dto.data.UserDto;
import com.codeit.playlist.domain.user.dto.data.UserSummary;
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

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PlaylistController.class,
        excludeFilters = {
                @ComponentScan.Filter(
                        type = FilterType.ASSIGNABLE_TYPE,
                        classes = com.codeit.playlist.domain.security.jwt.JwtAuthenticationFilter.class
                )
        })
@AutoConfigureMockMvc
@ImportAutoConfiguration(exclude = JpaConfig.class)
public class PlaylistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PlaylistService playlistService;

    @MockBean
    private PlaylistSubscriptionService playlistSubscriptionService;

    @MockBean
    private PlaylistContentService playlistContentService;

    UUID userId = UUID.randomUUID();

//    @AuthenticationPrincipal PlaylistUserDetails 로 들어갈 인증 객체 생성
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
    @DisplayName("플레이리스트 생성 성공")
    void createPlaylistSuccess() throws Exception {
        // given
        UUID ownerId = UUID.randomUUID();
        Authentication authentication = createAuthentication(ownerId);

        PlaylistCreateRequest request = new PlaylistCreateRequest("제목", "설명");

        UUID playlistId = UUID.randomUUID();
        PlaylistDto responseDto = new PlaylistDto(
                playlistId,
                null,
                "제목",
                "설명",
                LocalDateTime.now(),
                0L,
                false,
                List.of()
        );

        given(playlistService.createPlaylist(any(PlaylistCreateRequest.class), eq(ownerId)))
                .willReturn(responseDto);

        // when & then
        mockMvc.perform(post("/api/playlists")
                        .with(authentication(authentication))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(playlistId.toString()))
                .andExpect(jsonPath("$.title").value("제목"));

        then(playlistService).should()
                .createPlaylist(any(PlaylistCreateRequest.class), eq(ownerId));
    }

    @Test
    @DisplayName("플레이리스트 생성 실패 - 인증 없으면 401")
    void createPlaylistFailUnauthenticated() throws Exception {
        // given
        PlaylistCreateRequest request = new PlaylistCreateRequest("제목", "설명");

        // when & then
        mockMvc.perform(post("/api/playlists")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        then(playlistService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("플레이리스트 생성 실패 - 검증 에러(빈 제목)")
    void createPlaylistFailValidation() throws Exception {
        // given
        UUID ownerId = UUID.randomUUID();
        Authentication auth = createAuthentication(ownerId);

        PlaylistCreateRequest invalidRequest = new PlaylistCreateRequest("", "설명");

        // when & then
        mockMvc.perform(post("/api/playlists")
                        .with(authentication(auth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        then(playlistService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("플레이리스트 수정 성공")
    void updatePlaylistSuccess() throws Exception {
        // given
        UUID currentUserId = UUID.randomUUID();
        Authentication auth = createAuthentication(currentUserId);

        UUID playlistId = UUID.randomUUID();
        PlaylistUpdateRequest request = new PlaylistUpdateRequest("수정제목", "수정설명");

        PlaylistDto updatedDto = new PlaylistDto(
                playlistId,
                new UserSummary(currentUserId, "owner", null),
                "수정제목",
                "수정설명",
                LocalDateTime.now(),
                5L,
                false,
                List.of()
        );

        given(playlistService.updatePlaylist(eq(playlistId), any(PlaylistUpdateRequest.class), eq(currentUserId)))
                .willReturn(updatedDto);

        // when & then
        mockMvc.perform(patch("/api/playlists/{playlistId}", playlistId)
                        .with(authentication(auth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(playlistId.toString()))
                .andExpect(jsonPath("$.title").value("수정제목"))
                .andExpect(jsonPath("$.description").value("수정설명"));

        then(playlistService).should()
                .updatePlaylist(eq(playlistId), any(PlaylistUpdateRequest.class), eq(currentUserId));
    }

    @Test
    @DisplayName("플레이리스트 수정 실패 - 인증 없으면 401")
    void updatePlaylistFailUnauthenticated() throws Exception {
        // given
        UUID playlistId = UUID.randomUUID();
        PlaylistUpdateRequest request = new PlaylistUpdateRequest("수정제목", "수정설명");

        // when & then
        mockMvc.perform(patch("/api/playlists/{playlistId}", playlistId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        then(playlistService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("플레이리스트 수정 실패 - 검증 에러(빈 설명)")
    void updatePlaylistFailValidation() throws Exception {
        // given
        UUID currentUserId = UUID.randomUUID();
        Authentication auth = createAuthentication(currentUserId);

        UUID playlistId = UUID.randomUUID();
        PlaylistUpdateRequest invalidRequest = new PlaylistUpdateRequest("수정제목", "");

        // when & then
        mockMvc.perform(patch("/api/playlists/{playlistId}", playlistId)
                        .with(authentication(auth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        then(playlistService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("플레이리스트 삭제 성공")
    void deletePlaylistSuccess() throws Exception {
        // given
        UUID currentUserId = UUID.randomUUID();
        Authentication auth = createAuthentication(currentUserId);

        UUID playlistId = UUID.randomUUID();

        // when & then
        mockMvc.perform(delete("/api/playlists/{playlistId}", playlistId)
                        .with(authentication(auth))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        then(playlistService).should()
                .deletePlaylist(eq(playlistId), eq(currentUserId));
    }

    @Test
    @DisplayName("플레이리스트 삭제 실패 - 인증 없으면 401")
    void deletePlaylistFailUnauthenticated() throws Exception {
        // given
        UUID playlistId = UUID.randomUUID();

        // when & then
        mockMvc.perform(delete("/api/playlists/{playlistId}", playlistId)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());

        then(playlistService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("플레이리스트 목록 조회 성공")
    void getPlaylistsSuccess() throws Exception {
        // given
        int limit = 10;
        PlaylistSortBy sortBy = PlaylistSortBy.updatedAt;
        SortDirection sortDirection = SortDirection.DESCENDING;

        UUID playlistId = UUID.randomUUID();
        PlaylistDto dto = new PlaylistDto(
                playlistId,
                new UserSummary(UUID.randomUUID(), "owner", null),
                "플리제목",
                "플리설명",
                LocalDateTime.now(),
                3L,
                false,
                List.of()
        );

        CursorResponsePlaylistDto cursorResponse = new CursorResponsePlaylistDto(
                List.of(dto),
                "next-cursor",
                UUID.randomUUID(),
                true,
                100L,
                sortBy,
                sortDirection
        );

        given(playlistService.findPlaylists(
                any(), any(), any(), any(), any(), eq(limit), eq(sortBy), eq(sortDirection)
        )).willReturn(cursorResponse);

        // when & then
        mockMvc.perform(get("/api/playlists")
                        .with(authentication(createAuthentication(userId)))
                        .param("limit", String.valueOf(limit))
                        .param("sortBy", sortBy.name())
                        .param("sortDirection", sortDirection.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(playlistId.toString()))
                .andExpect(jsonPath("$.totalCount").value(100L))
                .andExpect(jsonPath("$.hasNext").value(true));

        then(playlistService).should()
                .findPlaylists(any(), any(), any(), any(), any(), eq(limit), eq(sortBy), eq(sortDirection));
    }

    @Test
    @DisplayName("플레이리스트 목록 조회 실패 - 인증 없으면 401")
    void getPlaylistsFailUnauthenticated() throws Exception {
        // when & then
        mockMvc.perform(get("/api/playlists")
                        .param("limit", "10")
                        .param("sortBy", "updatedAt")
                        .param("sortDirection", SortDirection.DESCENDING.name()))
                .andExpect(status().isUnauthorized());

        then(playlistService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("플레이리스트 단건 조회 성공")
    void getPlaylistSuccess() throws Exception {
        // given
        UUID playlistId = UUID.randomUUID();
        Authentication auth = createAuthentication(userId);

        PlaylistDto dto = new PlaylistDto(
                playlistId,
                new UserSummary(UUID.randomUUID(), "owner", null),
                "단건제목",
                "단건설명",
                LocalDateTime.now(),
                2L,
                false,
                List.of()
        );

        given(playlistService.getPlaylist(eq(playlistId)))
                .willReturn(dto);

        // when & then
        mockMvc.perform(get("/api/playlists/{playlistId}", playlistId)
                .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(playlistId.toString()))
                .andExpect(jsonPath("$.title").value("단건제목"));

        then(playlistService).should()
                .getPlaylist(eq(playlistId));
    }

    @Test
    @DisplayName("플레이리스트 단건 조회 실패 - 인증 없으면 401")
    void getPlaylistFailUnauthenticated() throws Exception {
        // given
        UUID playlistId = UUID.randomUUID();

        // when & then
        mockMvc.perform(get("/api/playlists/{playlistId}", playlistId))
                .andExpect(status().isUnauthorized());

        // 서비스는 호출되면 안 됨
        then(playlistService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("플레이리스트 구독 성공")
    void playlistSubscriptionSuccess() throws Exception {
        // given
        UUID currentUserId = UUID.randomUUID();
        Authentication auth = createAuthentication(currentUserId);
        UUID playlistId = UUID.randomUUID();

        // when & then
        mockMvc.perform(post("/api/playlists/{playlistId}/subscription", playlistId)
                        .with(authentication(auth))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        then(playlistSubscriptionService).should()
                .subscribe(eq(playlistId), eq(currentUserId));
    }

    @Test
    @DisplayName("플레이리스트 구독 실패 - 인증 없으면 401")
    void playlistSubscriptionFailUnauthenticated() throws Exception {
        // given
        UUID playlistId = UUID.randomUUID();

        // when & then
        mockMvc.perform(post("/api/playlists/{playlistId}/subscription", playlistId)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());

        then(playlistSubscriptionService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("플레이리스트 구독 해제 성공")
    void playlistUnSubscriptionSuccess() throws Exception {
        // given
        UUID currentUserId = UUID.randomUUID();
        Authentication auth = createAuthentication(currentUserId);
        UUID playlistId = UUID.randomUUID();

        // when & then
        mockMvc.perform(delete("/api/playlists/{playlistId}/subscription", playlistId)
                        .with(authentication(auth))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        then(playlistSubscriptionService).should()
                .unsubscribe(eq(playlistId), eq(currentUserId));
    }

    @Test
    @DisplayName("플레이리스트 구독 해제 실패 - 인증 없으면 401")
    void playlistUnSubscriptionFailUnauthenticated() throws Exception {
        // given
        UUID playlistId = UUID.randomUUID();

        // when & then
        mockMvc.perform(delete("/api/playlists/{playlistId}/subscription", playlistId)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());

        then(playlistSubscriptionService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("플레이리스트에 콘텐츠 추가 성공")
    void addContentToPlaylistSuccess() throws Exception {
        // given
        UUID currentUserId = UUID.randomUUID();
        Authentication auth = createAuthentication(currentUserId);
        UUID playlistId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();

        // when & then
        mockMvc.perform(post("/api/playlists/{playlistId}/contents/{contentId}", playlistId, contentId)
                        .with(authentication(auth))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        then(playlistContentService).should()
                .addContentToPlaylist(eq(playlistId), eq(contentId), eq(currentUserId));
    }

    @Test
    @DisplayName("플레이리스트에 콘텐츠 추가 실패 - 인증 없으면 401")
    void addContentToPlaylistFailUnauthenticated() throws Exception {
        // given
        UUID playlistId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();

        // when & then
        mockMvc.perform(post("/api/playlists/{playlistId}/contents/{contentId}", playlistId, contentId)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());

        then(playlistContentService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("플레이리스트에서 콘텐츠 삭제 성공")
    void removeContentFromPlaylistSuccess() throws Exception {
        // given
        UUID currentUserId = UUID.randomUUID();
        Authentication auth = createAuthentication(currentUserId);
        UUID playlistId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();

        // when & then
        mockMvc.perform(delete("/api/playlists/{playlistId}/contents/{contentId}", playlistId, contentId)
                        .with(authentication(auth))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        then(playlistContentService).should()
                .removeContentFromPlaylist(eq(playlistId), eq(contentId), eq(currentUserId));
    }

    @Test
    @DisplayName("플레이리스트에서 콘텐츠 삭제 실패 - 인증 없으면 401")
    void removeContentFromPlaylistFailUnauthenticated() throws Exception {
        // given
        UUID playlistId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();

        // when & then
        mockMvc.perform(delete("/api/playlists/{playlistId}/contents/{contentId}", playlistId, contentId)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());

        then(playlistContentService).shouldHaveNoInteractions();
    }
}
