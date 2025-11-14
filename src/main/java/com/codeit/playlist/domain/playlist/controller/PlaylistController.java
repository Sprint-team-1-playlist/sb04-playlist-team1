package com.codeit.playlist.domain.playlist.controller;

import com.codeit.playlist.domain.playlist.dto.data.PlaylistDto;
import com.codeit.playlist.domain.playlist.dto.request.PlaylistCreateRequest;
import com.codeit.playlist.domain.playlist.dto.request.PlaylistUpdateRequest;
import com.codeit.playlist.domain.playlist.service.PlaylistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/playlists")
@RequiredArgsConstructor
public class PlaylistController {

    private final PlaylistService playlistService;

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

}
