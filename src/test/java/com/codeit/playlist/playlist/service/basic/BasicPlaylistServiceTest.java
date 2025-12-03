package com.codeit.playlist.playlist.service.basic;

import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.content.dto.data.ContentSummary;
import com.codeit.playlist.domain.follow.repository.FollowRepository;
import com.codeit.playlist.domain.playlist.dto.data.PlaylistDto;
import com.codeit.playlist.domain.playlist.dto.data.PlaylistSortBy;
import com.codeit.playlist.domain.playlist.dto.request.PlaylistCreateRequest;
import com.codeit.playlist.domain.playlist.dto.request.PlaylistUpdateRequest;
import com.codeit.playlist.domain.playlist.dto.response.CursorResponsePlaylistDto;
import com.codeit.playlist.domain.playlist.entity.Playlist;
import com.codeit.playlist.domain.playlist.exception.PlaylistAccessDeniedException;
import com.codeit.playlist.domain.playlist.exception.PlaylistNotFoundException;
import com.codeit.playlist.domain.playlist.mapper.PlaylistMapper;
import com.codeit.playlist.domain.playlist.repository.PlaylistRepository;
import com.codeit.playlist.domain.playlist.service.basic.BasicPlaylistService;
import com.codeit.playlist.domain.user.dto.data.UserSummary;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.exception.UserNotFoundException;
import com.codeit.playlist.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import static org.mockito.Mockito.never;
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

    @Mock
    private FollowRepository followRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    BasicPlaylistService basicPlaylistService;

    @BeforeEach
    void setUp() {
        basicPlaylistService = new BasicPlaylistService(playlistRepository, userRepository, playlistMapper,
                                                        followRepository, objectMapper, kafkaTemplate);
    }

    @Test
    @DisplayName("ownerId가 있을 때 사용자가 포함된 플레이리스트 생성 성공")
    void playlistCreateWithOwnerId() {
        //given
        UUID ownerId = UUID.randomUUID();
        PlaylistCreateRequest request = new PlaylistCreateRequest("제목", "설명");

        User owner = mock(User.class);
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));

        Playlist saved = mock(Playlist.class);
        when(playlistRepository.save(any(Playlist.class))).thenReturn(saved);

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
        verify(playlistRepository).save(any(Playlist.class));
        verify(playlistMapper).toDto(saved);
        verifyNoMoreInteractions(userRepository, playlistRepository, playlistMapper);
    }

    @Test
    @DisplayName("ownerId가 있지만 DB에 사용자 행이 없어 EntityNotFoundException 발생")
    void failToCreateWithOwnerIdWhenUserMissing() {
        //given
        UUID ownerId = UUID.randomUUID();
        PlaylistCreateRequest request = new PlaylistCreateRequest("제목", "설명");
        when(userRepository.findById(ownerId)).thenReturn(Optional.empty());

        // when
        UserNotFoundException ex = assertThrows(
                UserNotFoundException.class,
                () -> basicPlaylistService.createPlaylist(request, ownerId)
        );

        // then
        assertTrue(ex.getMessage().contains("사용자 정보가 없습니다."));
        verify(userRepository).findById(ownerId);
        verifyNoInteractions(playlistMapper, playlistRepository);
    }

    @Test
    @DisplayName("소유자ID와 접속자ID가 같을 때 플레이리스트 수정 성공")
    void updatePlaylistSuccessWhenOwnerEqualsCurrentUser() {
        // given
        UUID playlistId = UUID.randomUUID();
        UUID currentUserId = CURRENT_USER_ID;
        User owner = createUserWithId(currentUserId);  // owner == currentUser

        Playlist playlist = new Playlist(owner, "old title", "old description");

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
        PlaylistDto result = basicPlaylistService.updatePlaylist(playlistId, request, currentUserId);

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

        Playlist playlist = new Playlist(owner, "old title", "old description");

        PlaylistUpdateRequest request =
                new PlaylistUpdateRequest("new title", "new description");

        UUID currentUserId = CURRENT_USER_ID;

        given(playlistRepository.findById(playlistId))
                .willReturn(Optional.of(playlist));

        // when & then
        assertThrows(PlaylistAccessDeniedException.class,
                () -> basicPlaylistService.updatePlaylist(playlistId, request, currentUserId));

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
        PlaylistSortBy sortBy = PlaylistSortBy.updatedAt;
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
        assertThat(result.sortBy()).isEqualTo(sortBy);
        assertThat(result.sortDirection()).isEqualTo(SortDirection.DESCENDING);

        verify(playlistRepository).searchPlaylists(
                eq(keywordLike),
                eq(ownerId),
                eq(subscriberId),
                eq(false),       // cursor/idAfter 없으므로 hasCursor=false
                isNull(),        // cursorId=null
                eq(false),       // DESCENDING → asc=false
                eq(sortBy),
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
                PlaylistSortBy.updatedAt,
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
                eq(PlaylistSortBy.updatedAt),
                any()
        );
    }

    @Test
    @DisplayName("softDeletePlaylist 성공 - 소유자와 요청자가 같고 아직 삭제되지 않은 경우")
    void softDeletePlaylistSuccessWhenOwnerEquals() {
        //given
        UUID ownerId = UUID.randomUUID();
        UUID playlistId = UUID.randomUUID();

        User owner = mock(User.class);
        when(owner.getId()).thenReturn(ownerId);

        Playlist playlist = mock(Playlist.class);
        when(playlist.getOwner()).thenReturn(owner);

        // 아직 삭제되지 않은 플레이리스트
        when(playlistRepository.findByIdAndDeletedAtIsNull(playlistId))
                .thenReturn(Optional.of(playlist));

        // 실제 soft delete 쿼리 결과 row 1개 업데이트
        when(playlistRepository.softDeleteById(playlistId)).thenReturn(1);

        //when
        basicPlaylistService.softDeletePlaylist(playlistId, ownerId);

        //then
        verify(playlistRepository).findByIdAndDeletedAtIsNull(playlistId);
        verify(playlistRepository).softDeleteById(playlistId);
        verifyNoMoreInteractions(playlistRepository);
    }

    @Test
    @DisplayName("softDeletePlaylist 실패 - 존재하지 않거나 이미 삭제된 경우 PlaylistNotFoundException 발생")
    void softDeletePlaylistFailWhenPlaylistNotFoundOrAlreadyDeleted() {
        // given
        UUID playlistId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        when(playlistRepository.findByIdAndDeletedAtIsNull(playlistId))
                .thenReturn(Optional.empty());

        // when & then
        assertThrows(
                PlaylistNotFoundException.class,
                () -> basicPlaylistService.softDeletePlaylist(playlistId, requesterId)
        );

        verify(playlistRepository).findByIdAndDeletedAtIsNull(playlistId);
        verify(playlistRepository, never()).softDeleteById(any());
    }

    @Test
    @DisplayName("softDeletePlaylist 실패 - 소유자와 요청자가 다르면 PlaylistAccessDeniedException 발생")
    void softDeletePlaylistFailWhenOwnerDifferentFromRequester() {
        // given
        UUID playlistId = UUID.randomUUID();

        UUID ownerId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID requesterId = CURRENT_USER_ID;

        User owner = mock(User.class);
        when(owner.getId()).thenReturn(ownerId);

        Playlist playlist = mock(Playlist.class);
        when(playlist.getOwner()).thenReturn(owner);

        when(playlistRepository.findByIdAndDeletedAtIsNull(playlistId))
                .thenReturn(Optional.of(playlist));

        // when & then
        assertThrows(
                PlaylistAccessDeniedException.class,
                () -> basicPlaylistService.softDeletePlaylist(playlistId, requesterId)
        );

        verify(playlistRepository).findByIdAndDeletedAtIsNull(playlistId);
        verify(playlistRepository, never()).softDeleteById(any());
    }

    @Test
    @DisplayName("getPlaylist 성공 - 플레이리스트 단건 조회 성공 후 DTO로 변환")
    void getPlaylistSuccess() {
        //given
        UUID playlistId = UUID.randomUUID();

        Playlist playlist = mock(Playlist.class);

        given(playlistRepository.findWithDetailsById(playlistId))
                .willReturn(Optional.of(playlist));

        UserSummary ownerSummary = new UserSummary(UUID.randomUUID(), "이름", "profile.png");

        ContentSummary content1 = new ContentSummary(UUID.randomUUID(), "MOVIE", "영화1", "테스트용 영화",
                "썸네일.png", List.of("태그1"), 4.5, 10);

        ContentSummary content2 = new ContentSummary(UUID.randomUUID(), "Soccer", "축구", "테스트용 경기",
                "썸네일2.png", List.of("태그2"), 4.0, 8);

        PlaylistDto mappedDto = new PlaylistDto(playlistId, ownerSummary, "테스트 플리", "테스트용",
                LocalDateTime.now(), 2L, false, List.of(content1, content2));

        given(playlistMapper.toDto(playlist)).willReturn(mappedDto);

        //when
        PlaylistDto result = basicPlaylistService.getPlaylist(playlistId);

        //then
        assertThat(result.id()).isEqualTo(mappedDto.id());
        assertThat(result.title()).isEqualTo("테스트 플리");
        assertThat(result.description()).isEqualTo("테스트용");
        assertThat(result.owner()).isEqualTo(ownerSummary);
        assertThat(result.subscriberCount()).isEqualTo(2L);
        assertThat(result.contents())
                .hasSize(2)
                .extracting(ContentSummary::title)
                .containsExactlyInAnyOrder("영화1", "축구");

        assertThat(result.subscribedByMe()).isFalse();

        then(playlistRepository).should().findWithDetailsById(playlistId);
        then(playlistMapper).should().toDto(playlist);
    }

    @Test
    @DisplayName("getPlaylist 실패 - 존재하지 않는 ID면 PlaylistNotFoundException이 발생한다")
    void getPlaylistFailWhenNotFound() {
        // given
        UUID playlistId = UUID.randomUUID();

        given(playlistRepository.findWithDetailsById(playlistId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> basicPlaylistService.getPlaylist(playlistId))
                .isInstanceOf(PlaylistNotFoundException.class);

        then(playlistRepository).should().findWithDetailsById(playlistId);
        // 매퍼는 호출되면 안 됨
        then(playlistMapper).shouldHaveNoInteractions();
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
