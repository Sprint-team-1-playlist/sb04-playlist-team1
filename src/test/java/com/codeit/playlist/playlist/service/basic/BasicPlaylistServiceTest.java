package com.codeit.playlist.playlist.service.basic;

import com.codeit.playlist.domain.playlist.dto.data.PlaylistDto;
import com.codeit.playlist.domain.playlist.dto.request.PlaylistCreateRequest;
import com.codeit.playlist.domain.playlist.entity.Playlist;
import com.codeit.playlist.domain.playlist.mapper.PlaylistMapper;
import com.codeit.playlist.domain.playlist.repository.PlaylistRepository;
import com.codeit.playlist.domain.playlist.service.basic.BasicPlaylistService;
import com.codeit.playlist.domain.user.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BasicPlaylistServiceTest {

    @Mock
    private PlaylistRepository playlistRepository;

    @Mock
    private PlaylistMapper playlistMapper;

    @Mock
    EntityManager em;

    @InjectMocks
    BasicPlaylistService basicPlaylistService;

    @Test
    @DisplayName("ownerId가 있을 때 사용자가 포함된 플레이리스트 생성 성공")
    void playlistCreateWithOwnerId() {
        //given
        UUID ownerId = UUID.randomUUID();
        PlaylistCreateRequest request = new PlaylistCreateRequest("제목", "설명");

        User ownerRef = mock(User.class);
        when(em.getReference(User.class, ownerId)).thenReturn(ownerRef);

        Playlist mapped = mock(Playlist.class);
        when(playlistMapper.toEntity(request, ownerRef)).thenReturn(mapped);

        Playlist saved = mock(Playlist.class);
        when(playlistRepository.save(mapped)).thenReturn(saved);

        PlaylistDto expected = new PlaylistDto(
                UUID.randomUUID(), /* owner */ null, "제목", "설명",
                null, 0L, false, List.of()
        );
        when(playlistMapper.toDto(saved)).thenReturn(expected);

        // when
        PlaylistDto actual = basicPlaylistService.createPlaylist(request, ownerId);

        // then
        assertSame(expected, actual);
        verify(em).getReference(User.class, ownerId);
        verify(playlistMapper).toEntity(request, ownerRef);
        verify(playlistMapper, never()).toEntity(request);
        verify(playlistRepository).save(mapped);
        verify(playlistMapper).toDto(saved);
        verifyNoMoreInteractions(em, playlistRepository, playlistMapper);
    }

    @Test
    @DisplayName("ownerId가 있지만 DB에 사용자 행이 없어 저장 시 FK 제약 위반 - 400 예외 발생")
    void failToCreatewithOwnerIdwhenUserRowMissing() {
        //given
        UUID ownerId = UUID.randomUUID();
        PlaylistCreateRequest request = new PlaylistCreateRequest("제목", "설명");

        User ownerRef = mock(User.class);
        when(em.getReference(User.class, ownerId)).thenReturn(ownerRef);

        Playlist mapped = mock(Playlist.class);
        when(playlistMapper.toEntity(request, ownerRef)).thenReturn(mapped);

        // DB 무결성 위반(존재하지 않는 FK)
        when(playlistRepository.save(mapped))
                .thenThrow(new DataIntegrityViolationException("사용자 행이 DB에 존재하지 않습니다."));

        // when
        DataIntegrityViolationException ex = assertThrows(
                DataIntegrityViolationException.class,
                () -> basicPlaylistService.createPlaylist(request, ownerId)
        );

        // then
        assertTrue(ex.getMessage().contains("사용자 행이 DB에 존재하지 않습니다."));
        verify(em).getReference(User.class, ownerId);
        verify(playlistMapper).toEntity(request, ownerRef);
        verify(playlistRepository).save(mapped);
        verify(playlistMapper, never()).toDto(any());
        verifyNoMoreInteractions(em, playlistRepository, playlistMapper);
    }

    @Test
    @DisplayName("ownerId가 있지만 title이 공백이어서 400 예외 발생")
    void failToCreatewhenTitleBlank() {
        // given
        UUID ownerId = UUID.randomUUID();
        PlaylistCreateRequest request = new PlaylistCreateRequest("   ", "설명");

        // when
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> basicPlaylistService.createPlaylist(request, ownerId)
        );

        // then
        assertTrue(ex.getMessage().contains("title"));
        verifyNoInteractions(em, playlistRepository, playlistMapper);
    }

    @Test
    @DisplayName("ownerId가 있지만 description이 공백이어서 400 예외 발생")
    void failToCreatewhenDescriptionBlank() {
        // given
        UUID ownerId = UUID.randomUUID();
        PlaylistCreateRequest request = new PlaylistCreateRequest("제목", "   ");

        // when
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> basicPlaylistService.createPlaylist(request, ownerId)
        );

        // then
        assertTrue(ex.getMessage().contains("description"));
        verifyNoInteractions(em, playlistRepository, playlistMapper);
    }

    @Test
    @DisplayName("ownerId가 없어 사용자가 포함되지 않은 플레이리스트 생성 성공")
    void playlistCreateWithoutOwnerId() {
        // given
        UUID ownerId = null;
        PlaylistCreateRequest request = new PlaylistCreateRequest("제목", "설명");

        Playlist mapped = mock(Playlist.class);
        when(playlistMapper.toEntity(request)).thenReturn(mapped);

        Playlist saved = mock(Playlist.class);
        when(playlistRepository.save(mapped)).thenReturn(saved);

        PlaylistDto expected = new PlaylistDto(
                UUID.randomUUID(), /* owner */ null, "제목", "설명",
                null, 0L, false, List.of()
        );
        when(playlistMapper.toDto(saved)).thenReturn(expected);

        // when
        PlaylistDto actual = basicPlaylistService.createPlaylist(request, ownerId);

        // then
        assertSame(expected, actual);
        verify(em, never()).getReference(any(), any());
        verify(playlistMapper).toEntity(request);
        verify(playlistMapper, never()).toEntity(eq(request), any(User.class));
        verify(playlistRepository).save(mapped);
        verify(playlistMapper).toDto(saved);
        verifyNoMoreInteractions(em, playlistRepository, playlistMapper);
    }

    @Test
    @DisplayName("ownerId가 없는 플레이리스트이지만 title이 공백이어서 400 예외 발생")
    void failToCreateWithoutOwnerIdwhenTitleBlank() {
        // given
        UUID ownerId = null;
        PlaylistCreateRequest request = new PlaylistCreateRequest("   ", "설명");

        // when
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> basicPlaylistService.createPlaylist(request, ownerId)
        );

        // then
        assertTrue(ex.getMessage().contains("title"));
        verifyNoInteractions(em, playlistRepository, playlistMapper);
    }

    @Test
    @DisplayName("ownerId가 없는 플레이리스트이지만 description이 공백이어서 400 예외 발생")
    void failToCreateWithoutOwnerIdwhenDescriptionBlank() {
        // given
        UUID ownerId = null;
        PlaylistCreateRequest request = new PlaylistCreateRequest("제목", "   ");

        // when
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> basicPlaylistService.createPlaylist(request, ownerId)
        );

        // then
        assertTrue(ex.getMessage().contains("description"));
        verifyNoInteractions(em, playlistRepository, playlistMapper);
    }
}
