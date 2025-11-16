package com.codeit.playlist.playlist.service.basic;

import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.playlist.dto.data.PlaylistDto;
import com.codeit.playlist.domain.playlist.dto.request.PlaylistCreateRequest;
import com.codeit.playlist.domain.playlist.dto.request.PlaylistUpdateRequest;
import com.codeit.playlist.domain.playlist.dto.response.CursorResponsePlaylistDto;
import com.codeit.playlist.domain.playlist.entity.Playlist;
import com.codeit.playlist.domain.playlist.exception.PlaylistAccessDeniedException;
import com.codeit.playlist.domain.playlist.mapper.PlaylistMapper;
import com.codeit.playlist.domain.playlist.repository.PlaylistRepository;
import com.codeit.playlist.domain.playlist.service.basic.BasicPlaylistService;
import com.codeit.playlist.domain.user.dto.data.UserSummary;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BasicPlaylistServiceTest {

    private static final UUID CURRENT_USER_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private PlaylistRepository playlistRepository;

    @Mock
    private PlaylistMapper playlistMapper;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    BasicPlaylistService basicPlaylistService;

    @BeforeEach
    void setUp() {
        basicPlaylistService = new BasicPlaylistService(playlistRepository, userRepository, playlistMapper);
    }

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

    @Test
    @DisplayName("소유자ID와 접속자ID가 같을 때 플레이리스트 수정 성공")
    void updatePlaylistSuccessWhenOwnerEqualsCurrentUser() {
        // given
        UUID playlistId = UUID.randomUUID();
        User owner = createUserWithId(CURRENT_USER_ID);  // owner == currentUser

        Playlist playlist = new Playlist(owner, "old title", "old description", 0L);

        PlaylistUpdateRequest request = new PlaylistUpdateRequest("new title", "new description");

        UserSummary ownerSummary = new UserSummary(
                CURRENT_USER_ID,
                "ownerName",
                null
        );

        PlaylistDto expectedDto = new PlaylistDto(
                playlistId,
                ownerSummary,
                "new title",
                "new description",
                LocalDateTime.now(),
                0L,
                false,
                List.of()
        );

        given(playlistRepository.findById(playlistId)).willReturn(Optional.of(playlist));
        given(playlistMapper.toDto(playlist)).willReturn(expectedDto);

        // when
        PlaylistDto result = basicPlaylistService.updatePlaylist(playlistId, request);

        // then
        assertThat(result.title()).isEqualTo("new title");
        assertThat(result.description()).isEqualTo("new description");

        assertThat(playlist.getTitle()).isEqualTo("new title");
        assertThat(playlist.getDescription()).isEqualTo("new description");

        then(playlistRepository).should().findById(playlistId);
        then(playlistMapper).should().toDto(playlist);
    }

    @Test
    @DisplayName("소유자ID와 접속자ID가 다를 때 플레이리스트 수정 시 PlaylistAccessDeniedException 발생")
    void updatePlaylistFailWhenOwnerIsDifferentFromCurrentUser() {

        UUID playlistId = UUID.randomUUID();

        UUID ownerId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        User owner = createUserWithId(ownerId);

        Playlist playlist = new Playlist(owner, "old title", "old description", 0L);

        PlaylistUpdateRequest request =
                new PlaylistUpdateRequest("new title", "new description");

        given(playlistRepository.findById(playlistId))
                .willReturn(Optional.of(playlist));

        // when & then
        assertThrows(PlaylistAccessDeniedException.class,
                () -> basicPlaylistService.updatePlaylist(playlistId, request));

        then(playlistRepository).should().findById(playlistId);
        then(playlistMapper).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("플레이리스트 목록 조회 성공 - 커서와 필터로 페이지 조회")
    void findPlaylistsSuccess() {
        //given
        String keywordLike = "영화";
        UUID ownerId = UUID.randomUUID();
        UUID subscriberId = UUID.randomUUID();
        String cursor = null;    // 첫 페이지
        UUID idAfter = null;
        int limit = 5;
        String sortBy = "updatedAt";
        SortDirection sortDirection = SortDirection.DESCENDING;

        Playlist playlist1 = mock(Playlist.class);
        Playlist playlist2 = mock(Playlist.class);

        UUID lastId = UUID.randomUUID();
        when(playlist2.getId()).thenReturn(lastId);

        List<Playlist> content = List.of(playlist1, playlist2);
        Slice<Playlist> slice = new SliceImpl<>(content, PageRequest.of(0, limit), true);

        when(playlistRepository.searchPlaylists(
                any(), any(), any(), anyBoolean(), any(), anyBoolean(), any(), any())
        ).thenReturn(slice);

        PlaylistDto dto1 = new PlaylistDto(UUID.randomUUID(),
                new UserSummary(UUID.randomUUID(), "owner1", null),
                "플리1",
                "설명1",
                LocalDateTime.now(),
                5L,
                false,
                List.of());
        PlaylistDto dto2 = new PlaylistDto(
                UUID.randomUUID(),
                new UserSummary(UUID.randomUUID(), "owner2", null),
                "플리2",
                "설명2",
                LocalDateTime.now(),
                3L,
                false,
                List.of()
        );

        when(playlistMapper.toDto(playlist1)).thenReturn(dto1);
        when(playlistMapper.toDto(playlist2)).thenReturn(dto2);

        when(playlistRepository.countPlaylists(any(), any(), any()))
                .thenReturn(10L);

        // when
        CursorResponsePlaylistDto result = basicPlaylistService.findPlaylists(
                keywordLike,
                ownerId,
                subscriberId,
                cursor,
                idAfter,
                limit,
                sortBy,
                sortDirection
        );

        // then
        assertThat(result.data()).containsExactly(dto1, dto2);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isEqualTo(lastId.toString());
        assertThat(result.nextIdAfter()).isEqualTo(lastId);
        assertThat(result.totalCount()).isEqualTo(10L);
        assertThat(result.sortBy()).isEqualTo("updatedAt");
        assertThat(result.sortDirection()).isEqualTo(SortDirection.DESCENDING);

        verify(playlistRepository).searchPlaylists(
                eq(keywordLike),
                eq(ownerId),
                eq(subscriberId),
                eq(false),       // cursor/idAfter 없으므로 hasCursor=false
                isNull(),        // cursorId=null
                eq(false),       // DESCENDING → asc=false
                eq("updatedAt"),
                any()
        );
        verify(playlistRepository).countPlaylists(keywordLike, ownerId, subscriberId);
    }

    @Test
    @DisplayName("findPlaylists 실패 케이스 - 잘못된 cursor 문자열이 넘어와도 예외 없이 첫 페이지로 처리")
    void findPlaylistsInvalidCursorDoesNotThrow() {
        // given
        String invalidCursor = "not-a-uuid";
        int limit = 5;

        // Slice empty
        Slice<Playlist> slice = new SliceImpl<>(List.of(), PageRequest.of(0, limit), false);
        when(playlistRepository.searchPlaylists(
                any(), any(), any(), anyBoolean(), any(), anyBoolean(), any(), any())
        ).thenReturn(slice);
        when(playlistRepository.countPlaylists(any(), any(), any()))
                .thenReturn(0L);

        // when
        CursorResponsePlaylistDto result = basicPlaylistService.findPlaylists(
                null,
                null,
                null,
                invalidCursor,  // 잘못된 커서
                null,
                limit,
                "updatedAt",
                SortDirection.ASCENDING
        );

        // then
        assertThat(result.data()).isEmpty();
        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursor()).isNull();
        assertThat(result.nextIdAfter()).isNull();
        assertThat(result.totalCount()).isEqualTo(0L);

        // 잘못된 커서라도 hasCursor=false, cursorId=null 로 호출되는지 확인
        verify(playlistRepository).searchPlaylists(
                isNull(), // keyword
                isNull(), // owner
                isNull(), // subscriber
                eq(false),
                isNull(),
                eq(true),
                eq("updatedAt"),
                any()
        );
    }

    private User createUserWithId(UUID id) {
        try {
            Constructor<User> constructor = User.class.getDeclaredConstructor();
            constructor.setAccessible(true);   // private/protected 생성자 열기
            User user = constructor.newInstance();

            ReflectionTestUtils.setField(user, "id", id);
            return user;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
