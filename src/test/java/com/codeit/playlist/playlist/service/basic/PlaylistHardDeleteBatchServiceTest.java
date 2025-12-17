package com.codeit.playlist.playlist.service.basic;

import com.codeit.playlist.domain.playlist.entity.Playlist;
import com.codeit.playlist.domain.playlist.repository.PlaylistRepository;
import com.codeit.playlist.domain.playlist.service.basic.PlaylistHardDeleteBatchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PlaylistHardDeleteBatchServiceTest {

    @Mock
    private PlaylistRepository playlistRepository;

    @InjectMocks
    private PlaylistHardDeleteBatchService batchService;

    @Test
    @DisplayName("hardDeletedPlaylists - 7일 지난 soft delete 대상이 없으면 delete가 호출되지 않는다")
    void hardDeletedPlaylistsNoTargets() {
        // given
        when(playlistRepository.findAllDeletedBefore(any(Instant.class)))
                .thenReturn(List.of());

        // when
        batchService.hardDeletedPlaylists();

        // then
        verify(playlistRepository).findAllDeletedBefore(any(Instant.class));
        verify(playlistRepository, never()).delete(any(Playlist.class));
    }

    @Test
    @DisplayName("hardDeletedPlaylists - 7일 지난 soft delete 대상만 delete가 호출된다")
    void hardDeletedPlaylistsDeleteTargets() {
        // given
        Playlist p1 = mock(Playlist.class);
        Playlist p2 = mock(Playlist.class);
        List<Playlist> targets = List.of(p1, p2);

        when(playlistRepository.findAllDeletedBefore(any(Instant.class)))
                .thenReturn(targets);

        // when
        batchService.hardDeletedPlaylists();

        // then
        verify(playlistRepository).findAllDeletedBefore(any(Instant.class));
        verify(playlistRepository).delete(p1);
        verify(playlistRepository).delete(p2);
        verifyNoMoreInteractions(playlistRepository);
    }
}
