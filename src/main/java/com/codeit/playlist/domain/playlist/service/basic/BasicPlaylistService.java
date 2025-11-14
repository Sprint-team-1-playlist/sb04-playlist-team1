package com.codeit.playlist.domain.playlist.service.basic;

import com.codeit.playlist.domain.playlist.dto.data.PlaylistDto;
import com.codeit.playlist.domain.playlist.dto.request.PlaylistCreateRequest;
import com.codeit.playlist.domain.playlist.dto.request.PlaylistUpdateRequest;
import com.codeit.playlist.domain.playlist.entity.Playlist;
import com.codeit.playlist.domain.playlist.exception.PlaylistAccessDeniedException;
import com.codeit.playlist.domain.playlist.exception.PlaylistNotFoundException;
import com.codeit.playlist.domain.playlist.mapper.PlaylistMapper;
import com.codeit.playlist.domain.playlist.repository.PlaylistRepository;
import com.codeit.playlist.domain.playlist.service.PlaylistService;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BasicPlaylistService implements PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final UserRepository userRepository;
    private final PlaylistMapper playlistMapper;

    @Transactional
    @Override
    public PlaylistDto createPlaylist(PlaylistCreateRequest request, UUID ownerId) {

        log.debug("[플레이리스트] 생성 요청: ownerId={}, title={}", ownerId, request.title());

        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다. ID=" + ownerId));
        Playlist playlist = playlistMapper.toEntity(request, owner);

        Playlist saved = playlistRepository.save(playlist);

        PlaylistDto dto = playlistMapper.toDto(saved);

        log.info("[플레이리스트] 생성 완료: id={}", dto.id());
        return dto;
    }

    @Transactional
    @Override
    public PlaylistDto updatePlaylist(UUID playlistId, PlaylistUpdateRequest request) {
        log.debug("[플레이리스트] 수정 시작: playlistId={}", playlistId);

        //플레이리스트 조회
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> PlaylistNotFoundException.withId(playlistId));

        // 2. 현재 로그인한 사용자 ID 조회(Security 생성 시 교체 예정)
        UUID currentUserId = getCurrentUserIdForTest();

        //3. 소유자 검증
        UUID ownerId = playlist.getOwner().getId();

        log.debug("[플레이리스트] 소유자 검증: ownerId={}, currentUserId={}", ownerId, currentUserId);

        if (!ownerId.equals(currentUserId)) {
            throw PlaylistAccessDeniedException.withIds(playlist.getId(), ownerId, currentUserId);
        }

        playlist.updateInfo(request.title(), request.description());

        log.info("[플레이리스트] 수정 성공: playlistId={}", playlistId);

        return playlistMapper.toDto(playlist);
    }

    /*TODO : 시큐리티 구현 후 SecurityContext 에서 현재 사용자 ID 를 꺼내도록 변경 예정*/
    private UUID getCurrentUserIdForTest() {
        return UUID.fromString("11111111-1111-1111-1111-111111111111");
    }

}
