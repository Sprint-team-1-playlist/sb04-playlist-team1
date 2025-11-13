package com.codeit.playlist.domain.playlist.controller;

import com.codeit.playlist.domain.playlist.dto.data.PlaylistDto;
import com.codeit.playlist.domain.playlist.dto.request.PlaylistCreateRequest;
import com.codeit.playlist.domain.playlist.service.PlaylistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

}
