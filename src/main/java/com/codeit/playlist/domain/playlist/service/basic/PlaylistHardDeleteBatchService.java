package com.codeit.playlist.domain.playlist.service.basic;

import com.codeit.playlist.domain.playlist.entity.Playlist;
import com.codeit.playlist.domain.playlist.repository.PlaylistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PlaylistHardDeleteBatchService {

    private final PlaylistRepository playlistRepository;

    @Transactional
    public void hardDeleteExpiredPlaylists() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(7);

        List<Playlist> playlists = playlistRepository.findAllDeletedBefore(threshold);

        log.debug("[Batch] 하드 삭제 시작 - 대상: {}개", playlists.size());

        if (playlists.isEmpty()) {
            log.info("[Batch] 하드 삭제 대상 없음");
            return;
        }

        for (Playlist playlist : playlists) {
            playlistRepository.delete(playlist);
        }

        log.info("[Batch] 하드 삭제 완료");
    }
}
