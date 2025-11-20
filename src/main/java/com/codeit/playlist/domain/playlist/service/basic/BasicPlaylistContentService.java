package com.codeit.playlist.domain.playlist.service.basic;

import com.codeit.playlist.domain.content.entity.Content;
import com.codeit.playlist.domain.content.exception.ContentNotFoundException;
import com.codeit.playlist.domain.content.repository.ContentRepository;
import com.codeit.playlist.domain.playlist.entity.Playlist;
import com.codeit.playlist.domain.playlist.entity.PlaylistContent;
import com.codeit.playlist.domain.playlist.exception.playlist.PlaylistAccessDeniedException;
import com.codeit.playlist.domain.playlist.exception.playlistcontent.PlaylistContentAlreadyExistsException;
import com.codeit.playlist.domain.playlist.exception.playlistcontent.PlaylistContentNotFoundException;
import com.codeit.playlist.domain.playlist.exception.playlist.PlaylistNotFoundException;
import com.codeit.playlist.domain.playlist.repository.PlaylistContentRepository;
import com.codeit.playlist.domain.playlist.repository.PlaylistRepository;
import com.codeit.playlist.domain.playlist.service.PlaylistContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BasicPlaylistContentService implements PlaylistContentService {

    private final PlaylistRepository playlistRepository;
    private final ContentRepository contentRepository;
    private final PlaylistContentRepository playlistContentRepository;

    @Override
    @Transactional
    public void addContentToPlaylist(UUID playlistId, UUID contentId, UUID currentUserId) {

        log.debug("[플레이리스트] 콘텐츠 추가 시작 : playlistId={}, contentId={}, userId={}",
                playlistId, contentId, currentUserId);

        //삭제되지 않은 플레이리스트 조회
        Playlist playlist = playlistRepository.findByIdAndDeletedAtIsNull(playlistId)
                .orElseThrow(() -> {
                    log.error("[플레이리스트] 콘텐츠 추가 실패 : 존재하지 않는 playlistId={}", playlistId);
                    return PlaylistNotFoundException.withId(playlistId);
                });

        //소유자 검증
        validateOwner(playlist, currentUserId);

        //컨텐츠 조회
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> {
                    log.error("[플레이리스트] 콘텐츠 추가 실패 : 존재하지 않는 contentId={}", contentId);
                    return ContentNotFoundException.withId(contentId);
                });

        boolean exists = playlistContentRepository.existsByPlaylist_IdAndContent_Id(playlistId, contentId);

        //콘텐츠 중복 검사
        if (exists) {
            log.error("[플레이리스트] 콘텐츠 추가 실패 : 이미 존재하는 콘텐츠 playlistId={}, contentId={}",
                    playlistId, contentId);
            throw PlaylistContentAlreadyExistsException.withIds(playlistId, contentId);
        }

        try {
            PlaylistContent playlistContent = new PlaylistContent(playlist, content);
            playlistContentRepository.save(playlistContent);

        } catch (DataIntegrityViolationException e) {
            log.error("[플레이리스트] 콘텐츠 추가 실패 : DB unique 제약조건 위반 playlistId={}, contentId={}",
                    playlistId, contentId, e);
            throw PlaylistContentAlreadyExistsException.withIds(playlistId, contentId);
        }

        log.info("[플레이리스트] 콘텐츠 추가 : 저장 완료 및 종료 playlistId={}, contentId={}, userId={}",
                playlistId, contentId, currentUserId);
    }

    @Transactional
    @Override
    public void removeContentFromPlaylist(UUID playlistId, UUID contentId, UUID currentUserId) {

        log.debug("[플레이리스트] 콘텐츠 삭제 시작 - playlistId={}, contentId={}, currentUserId={}",
                playlistId, contentId, currentUserId);

        //삭제되지 않은 플레이리스트 조회
        Playlist playlist = playlistRepository.findByIdAndDeletedAtIsNull(playlistId)
                .orElseThrow(() -> {
                    log.error("[플레이리스트] 콘텐츠 삭제 실패 : 존재하지 않는 playlistId={}", playlistId);
                    return PlaylistNotFoundException.withId(playlistId);
                });

        //소유자 검증
        validateOwner(playlist, currentUserId);

        //플레이리스트-콘텐츠 매핑 조회
        PlaylistContent playlistContent = playlistContentRepository
                .findByPlaylist_IdAndContent_Id(playlistId, contentId)
                .orElseThrow(() -> {
                    log.error("[플레이리스트] 콘텐츠 삭제 매핑 조회 실패 - playlistId={}, contentId={}",
                            playlistId, contentId);
                    return PlaylistContentNotFoundException.withIds(playlistId, contentId);
                });

        //삭제
        playlistContentRepository.delete(playlistContent);

        log.info("[플레이리스트] 콘텐츠 삭제 : 삭제 완료 playlistId={}, contentId={}",
                playlistId, contentId);
    }

    //소유자 검증 로직
    private void validateOwner(Playlist playlist, UUID currentUserId) {
        UUID ownerId = playlist.getOwner().getId();
        if (!ownerId.equals(currentUserId)) {
            log.error("[플레이리스트] 소유자 검증 실패 - playlistOwnerId={}, currentUserId={}",
                    ownerId, currentUserId);
            throw PlaylistAccessDeniedException.withIds(playlist.getId(),ownerId, currentUserId);
        }
        log.debug("[플레이리스트] 소유자 검증 성공 : playlistOwnerId={}, currentUserId={}",
                playlist.getOwner().getId(), currentUserId);
    }
}
