package com.codeit.playlist.domain.playlist.service.basic;

import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.playlist.dto.data.PlaylistDto;
import com.codeit.playlist.domain.playlist.dto.request.PlaylistCreateRequest;
import com.codeit.playlist.domain.playlist.dto.request.PlaylistUpdateRequest;
import com.codeit.playlist.domain.playlist.dto.response.CursorResponsePlaylistDto;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BasicPlaylistService implements PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final UserRepository userRepository;
    private final PlaylistMapper playlistMapper;

    //플레이리스트 생성
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

    //플레이리스트 수정
    @Transactional
    @Override
    public PlaylistDto updatePlaylist(UUID playlistId, PlaylistUpdateRequest request, UUID currentUserId) {
        log.debug("[플레이리스트] 수정 시작: playlistId={}, currentUserId={}", playlistId, currentUserId);

        //플레이리스트 조회
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> PlaylistNotFoundException.withId(playlistId));

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

    //플레이리스트 논리 삭제
    @Transactional
    @Override
    public void softDeletePlaylist(UUID playlistId, UUID requesterUserId) {
        log.debug("[플레이리스트] 삭제 시작 : playlistId={}, requesterUserId={}", playlistId, requesterUserId);

        //삭제되지 않은 플레이리스트 조회
        Playlist playlist = playlistRepository.findByIdAndDeletedAtIsNull(playlistId)
                .orElseThrow(() -> {
                    log.error("[플레이리스트] 삭제 실패: 존재하지 않거나 이미 삭제됨 playlistId={}", playlistId);
                    return PlaylistNotFoundException.withId(playlistId);
                });

        //소유자 검증
        UUID ownerId = playlist.getOwner().getId();
        if (!ownerId.equals(requesterUserId)) {
            log.error("[플레이리스트] 논리 삭제 실패: 권한 없음 playlistId={}, ownerId={}, requester={}", playlistId, ownerId, requesterUserId);
            throw PlaylistAccessDeniedException.withPlaylistId(playlistId);
        }

        int deleted = playlistRepository.softDeleteById(playlistId);
        if (deleted == 0) {
            throw PlaylistNotFoundException.withId(playlistId);
        }

        log.info("[플레이리스트] 논리 삭제 성공 playlistId={}, requesterUserId={}",
                playlistId, requesterUserId);
    }

    //플레이리스트 일반 삭제(논리 삭제 호출)
    @Transactional
    @Override
    public void deletePlaylist(UUID playlistId, UUID requesterUserId) {
        log.debug("[플레이리스트] 삭제 시작 : playlistId = {}, requesterUserId = {}", playlistId, requesterUserId);
        softDeletePlaylist(playlistId, requesterUserId);
    }

    //플레이리스트 목록 조회
    @Transactional(readOnly = true)
    @Override
    public CursorResponsePlaylistDto findPlaylists(String keywordLike, UUID ownerIdEqual,
                                                   UUID subscriberIdEqual, String cursor,
                                                   UUID idAfter, int limit, String sortBy,
                                                   SortDirection sortDirection) {
        log.debug("[플레이리스트] 목록 조회 서비스 호출: " +
                        "keywordLike={}, ownerIdEqual={}, subscriberIdEqual={}, cursor={}, idAfter={}, limit={}, sortBy={}, sortDirection={}",
                keywordLike, ownerIdEqual, subscriberIdEqual, cursor, idAfter, limit, sortBy, sortDirection);

        //1. 파라미터 보정
        if(limit <= 0 || limit > 50) {
            limit = 10; //기본 페이지 크기(10개 가져옴)
        }

        //sortBy 허용값(updatedAt / subscriberCount)
        if (!"updatedAt".equals(sortBy) && !"subscriberCount".equals(sortBy)) {
            sortBy = "updatedAt";
        }

        boolean asc = (sortDirection == SortDirection.ASCENDING);

        // 3. 커서 해석 (cursor가 메인)
        UUID effectiveIdAfter = null;
        if (cursor != null && !cursor.isBlank()) {
            try {
                effectiveIdAfter = UUID.fromString(cursor);
            } catch (IllegalArgumentException e) {
                log.error("[플레이리스트] 잘못된 cursor 형식: cursor={}", cursor);
                effectiveIdAfter = null;
            }
        }

        //idAfter가 보조
        if (effectiveIdAfter == null && idAfter != null) {
            effectiveIdAfter = idAfter;
        }

        boolean hasCursor = (effectiveIdAfter != null);


        Pageable pageable = PageRequest.of(0, limit);

        Slice<Playlist> playlists = playlistRepository.searchPlaylists(
                keywordLike,
                ownerIdEqual,
                subscriberIdEqual,
                hasCursor,
                effectiveIdAfter,
                asc,
                sortBy,
                pageable
        );

        List<Playlist> content = playlists.getContent();
        boolean hasNext = playlists.hasNext();

        List<PlaylistDto> data = content.stream()
                .map(playlistMapper::toDto)
                .toList();

        // 6. nextCursor, nextIdAfter 계산
        String nextCursor = null;
        UUID nextIdAfter = null;

        if (hasNext && !content.isEmpty()) {
            Playlist last = content.get(content.size() - 1);
            nextCursor = last.getId().toString();   // 커서 = 마지막 id 문자열
            nextIdAfter = last.getId();
        }

        // 7. 필터 기준 전체 개수 조회
        long totalCount = playlistRepository.countPlaylists(
                keywordLike,
                ownerIdEqual,
                subscriberIdEqual
        );

        CursorResponsePlaylistDto response = new CursorResponsePlaylistDto(
                data,
                nextCursor,
                nextIdAfter,
                hasNext,
                totalCount,
                sortBy,
                sortDirection
        );

        log.debug("[플레이리스트] 목록 조회 서비스 완료: dataSize={}, hasNext={}, totalCount={}, nextCursor={}, nextIdAfter={}",
                data.size(), hasNext, totalCount, nextCursor, nextIdAfter);

        return response;
    }

    //플레이리스트 단건 조회
    @Transactional(readOnly = true)
    @Override
    public PlaylistDto getPlaylist(UUID playlistId) {
        log.debug("[플레이리스트] 단건 조회 시작: playlistId={}", playlistId);

        //플레이리스트와 연관 객체 로딩
        Playlist playlist = playlistRepository.findWithDetailsById(playlistId)
                .orElseThrow(() -> PlaylistNotFoundException.withId(playlistId));

        //로그인 사용자의 구독 여부(임시) -> 구독 구현 시 변경 예정
        boolean subscribedByMe = false;

        //Entity -> DTO
        PlaylistDto dto = playlistMapper.toDto(playlist);

        PlaylistDto result = new PlaylistDto(
                dto.id(),
                dto.owner(),
                dto.title(),
                dto.description(),
                dto.updatedAt(),
                dto.subscriberCount(),
                subscribedByMe,
                dto.contents()
        );

        log.info("[플레이리스트] 단건 조회 완료: playlistId={}", playlistId);
        return result;
    }

}
