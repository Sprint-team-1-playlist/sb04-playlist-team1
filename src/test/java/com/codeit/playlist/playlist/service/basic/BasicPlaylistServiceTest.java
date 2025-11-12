package com.codeit.playlist.playlist.service.basic;

import com.codeit.playlist.domain.playlist.dto.data.PlaylistDto;
import com.codeit.playlist.domain.playlist.dto.request.PlaylistCreateRequest;
import com.codeit.playlist.domain.playlist.entity.Playlist;
import com.codeit.playlist.domain.playlist.mapper.PlaylistMapper;
import com.codeit.playlist.domain.playlist.repository.PlaylistRepository;
import com.codeit.playlist.domain.playlist.service.basic.BasicPlaylistService;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;
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
    private UserRepository userRepository;

    @InjectMocks
    BasicPlaylistService basicPlaylistService;

    @Test
    @DisplayName("ownerId가 있을 때 사용자가 포함된 플레이리스트 생성 성공")
    void playlistCreateWithOwnerId() {
        //given
        UUID ownerId = UUID.randomUUID();
        PlaylistCreateRequest request = new PlaylistCreateRequest("제목", "설명");

        User owner = mock(User.class);
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));

        Playlist mapped = mock(Playlist.class);
        when(playlistMapper.toEntity(request, owner)).thenReturn(mapped);

        Playlist saved = mock(Playlist.class);
        when(playlistRepository.save(mapped)).thenReturn(saved);

        PlaylistDto expected = new PlaylistDto(
                UUID.randomUUID(), null, "제목", "설명",
                null, 0L, false, List.of()
        );
        when(playlistMapper.toDto(saved)).thenReturn(expected);

        // when
        PlaylistDto actual = basicPlaylistService.createPlaylist(request, ownerId);

        // then
        assertSame(expected, actual);
        verify(userRepository).findById(ownerId);
        verify(playlistMapper).toEntity(request, owner);
        verify(playlistRepository).save(mapped);
        verify(playlistMapper).toDto(saved);
        verifyNoMoreInteractions(userRepository, playlistRepository, playlistMapper);
    }

    @Test
    @DisplayName("ownerId가 있지만 DB에 사용자 행이 없어 EntityNotFoundException 발생")
    void failToCreatewithOwnerIdwhenUserMissing() {
        //given
        UUID ownerId = UUID.randomUUID();
        PlaylistCreateRequest request = new PlaylistCreateRequest("제목", "설명");
        when(userRepository.findById(ownerId)).thenReturn(Optional.empty());

        // when
        EntityNotFoundException ex = assertThrows(
                EntityNotFoundException.class,
                () -> basicPlaylistService.createPlaylist(request, ownerId)
        );

        // then
        assertTrue(ex.getMessage().contains("사용자를 찾을 수 없습니다"));
        verify(userRepository).findById(ownerId);
        verifyNoInteractions(playlistMapper, playlistRepository);
    }
}
