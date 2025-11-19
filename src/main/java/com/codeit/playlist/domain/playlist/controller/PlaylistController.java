package com.codeit.playlist.domain.playlist.controller;

import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.playlist.dto.data.PlaylistDto;
import com.codeit.playlist.domain.playlist.dto.request.PlaylistCreateRequest;
import com.codeit.playlist.domain.playlist.dto.request.PlaylistUpdateRequest;
import com.codeit.playlist.domain.playlist.dto.response.CursorResponsePlaylistDto;
import com.codeit.playlist.domain.playlist.service.PlaylistContentService;
import com.codeit.playlist.domain.playlist.service.PlaylistService;
import com.codeit.playlist.domain.playlist.service.PlaylistSubscriptionService;
import com.codeit.playlist.domain.security.PlaylistUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/playlists")
@RequiredArgsConstructor
public class PlaylistController {

    private final PlaylistService playlistService;
    private final PlaylistSubscriptionService playlistSubscriptionService;
    private final PlaylistContentService playlistContentService;

    //플레이리스트 생성
    @PostMapping
    public ResponseEntity<PlaylistDto> create(@Valid @RequestBody PlaylistCreateRequest request,
                                              @RequestHeader(value = "owner_id", required = false) UUID ownerId) { // 임시
        log.debug("[플레이리스트] 생성 요청: title={}, ownerId={}", request.title(), ownerId);

        PlaylistDto playlist = playlistService.createPlaylist(request, ownerId);

        log.info("플레이리스트 생성 완료 - id={}", playlist.id());

        return ResponseEntity.status(HttpStatus.CREATED).body(playlist);
    }

    //플레이리스트 수정
    @PatchMapping("/{playlistId}")
    public ResponseEntity<PlaylistDto> update(
            @PathVariable UUID playlistId,
            @Valid @RequestBody PlaylistUpdateRequest request) {
        log.debug("[플레이리스트] 수정 요청: id={}", playlistId);

        PlaylistDto updatedPlaylist = playlistService.updatePlaylist(playlistId, request);

        log.info("플레이리스트 수정 성공 - id={}", updatedPlaylist.id());

        return ResponseEntity.ok(updatedPlaylist);
    }

    //플레이리스트 삭제(플레이리스트 목록조회 선 구현을 위한 임시 비활성화)
    @DeleteMapping("/{playlistId}")
    public ResponseEntity<Void> delete(@PathVariable UUID playlistId) {

        log.debug("[플레이리스트] 삭제 요청: id={}", playlistId);
        playlistService.deletePlaylist(playlistId);

        log.info("플레이리스트 삭제 성공 - id={}", playlistId);
        return ResponseEntity.noContent().build();
    }

    //플레이리스트 목록 조회
    @GetMapping
    public ResponseEntity<CursorResponsePlaylistDto> getPlaylists(
            @RequestParam(required = false) String keywordLike,
            @RequestParam(required = false) UUID ownerIdEqual,
            @RequestParam(required = false) UUID subscriberIdEqual,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) UUID idAfter,
            @RequestParam int limit,
            @RequestParam(defaultValue = "DESCENDING") SortDirection sortDirection,  //DESENDING, ASCENDING
            @RequestParam String sortBy  //updatedAt, subscribeCount
            ) {
        log.debug("[플레이리스트] 플레이리스트 목록 조회 요청: " +
                "keywordLike={}, ownerIdEqual={}, subscriberIdEqual={}, cursor={}, idAfter={}, limit={}, sortBy={}, sortDirection={}",
                keywordLike, ownerIdEqual, subscriberIdEqual, cursor, idAfter, limit, sortBy, sortDirection);

        CursorResponsePlaylistDto playlists = playlistService.findPlaylists(
                keywordLike,
                ownerIdEqual,
                subscriberIdEqual,
                cursor,
                idAfter,
                limit,
                sortBy,
                sortDirection
        );

        log.info("[플레이리스트] 플레이리스트 목록 조회 완료: dataSize={}, hasNext={}, totalCount={}, nextCursor={} nextIdAfter={}",
                playlists.data().size(), playlists.hasNext(), playlists.totalCount(), playlists.nextCursor(), playlists.nextIdAfter());

        return ResponseEntity.ok(playlists);
    }

    //플레이리스트 단건 조회
    @GetMapping("/{playlistId}")
    public ResponseEntity<PlaylistDto> getPlaylist(@PathVariable UUID playlistId) {
        log.debug("[플레이리스트] 플레이리스트 단건 조회 시작: id={}", playlistId);

        PlaylistDto response = playlistService.getPlaylist(playlistId);

        log.info("[플레이리스트] 플레이리스트 단건 조회 성공: id={}", playlistId);
        return ResponseEntity.ok(response);
    }

    //플레이리스트 구독
    @PostMapping("/{playlistId}/subscription")
    public ResponseEntity<Void> playlistSubscription(@PathVariable UUID playlistId,
                                                     @RequestHeader("USER-ID") UUID subscriberId) {//subscriberId는 Security 구현시 삭제
        log.debug("[플레이리스트] 플레이리스트 구독 : playlistId={}, userId = {}", playlistId, subscriberId);

        playlistSubscriptionService.subscribe(playlistId, subscriberId);

        return ResponseEntity.ok().build();
    }

    //플레이리스트 구독 해제
    @DeleteMapping("/{playlistId}/subscription")
    public ResponseEntity<Void> playlistUnSubscription(@PathVariable UUID playlistId,
                                                       @RequestHeader("USER-ID") UUID subscriberId) {//subscriberId는 Security 구현시 삭제
        log.debug("[플레이리스트] 구독 해제 요청 : playlistId={}, userId={}", playlistId, subscriberId);

        playlistSubscriptionService.unsubscribe(playlistId, subscriberId);

        return ResponseEntity.noContent().build();
    }

    //플레이리스트에 콘텐츠 추가
    @PostMapping("/{playlistId}/contents/{contentId}")
    public ResponseEntity<Void> addContentToPlaylist(@PathVariable UUID playlistId,
                                                     @PathVariable UUID contentId,
                                                     @AuthenticationPrincipal PlaylistUserDetails userDetails) {

        log.debug("[플레이리스트] 콘텐츠 추가 시작 : playlistId={}, contentId={}, userId{}", playlistId, contentId, userDetails.getUserDto().id());

        UUID currentUserId = userDetails.getUserDto().id();

        playlistContentService.addContentToPlaylist(playlistId, contentId, currentUserId);

        log.info("[플레이리스트] 콘텐츠 추가 완료 : playlistId={}, contentId={}, userId={}", playlistId, contentId, currentUserId);

        return ResponseEntity.noContent().build();
    }

    //플레이리스트에 콘텐츠 삭제
    @DeleteMapping("/{playlistId}/contents/{contentId}")
    public ResponseEntity<Void> removeContentFromPlaylist(@PathVariable UUID playlistId,
                                                          @PathVariable UUID contentId,
                                                          @AuthenticationPrincipal PlaylistUserDetails userDetails) {

        log.debug("[플레이리스트] 콘텐츠 삭제 시작 : playlistId={}, contentId={}, userId{}",
                playlistId, contentId, userDetails.getUserDto().id());

        UUID currentUserId = userDetails.getUserDto().id();

        playlistContentService.removeContentFromPlaylist(playlistId, contentId, currentUserId);

        log.info("[플레이리스트] 콘텐츠 삭제 완료 : playlistId={}, contentId={}, userId={}",
                playlistId, contentId, currentUserId);

        return ResponseEntity.noContent().build();
    }
}
