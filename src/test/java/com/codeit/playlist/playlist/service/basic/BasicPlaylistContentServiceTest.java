package com.codeit.playlist.playlist.service.basic;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.codeit.playlist.domain.content.entity.Content;
import com.codeit.playlist.domain.content.entity.Type;
import com.codeit.playlist.domain.content.exception.ContentNotFoundException;
import com.codeit.playlist.domain.content.repository.ContentRepository;
import com.codeit.playlist.domain.playlist.entity.Playlist;
import com.codeit.playlist.domain.playlist.entity.PlaylistContent;
import com.codeit.playlist.domain.playlist.exception.PlaylistAccessDeniedException;
import com.codeit.playlist.domain.playlist.exception.PlaylistContentAlreadyExistsException;
import com.codeit.playlist.domain.playlist.exception.PlaylistContentNotFoundException;
import com.codeit.playlist.domain.playlist.exception.PlaylistNotFoundException;
import com.codeit.playlist.domain.playlist.repository.PlaylistContentRepository;
import com.codeit.playlist.domain.playlist.repository.PlaylistRepository;
import com.codeit.playlist.domain.playlist.repository.SubscribeRepository;
import com.codeit.playlist.domain.playlist.service.basic.BasicPlaylistContentService;
import com.codeit.playlist.domain.user.entity.Role;
import com.codeit.playlist.domain.user.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class BasicPlaylistContentServiceTest {
    private static final UUID PLAYLIST_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CONTENT_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID OWNER_ID =
            UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID OTHER_USER_ID =
            UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Mock
    private PlaylistRepository playlistRepository;

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private PlaylistContentRepository playlistContentRepository;

    @Mock
    private SubscribeRepository subscribeRepository;

    @InjectMocks
    private BasicPlaylistContentService basicPlaylistContentService;

    @Test
    @DisplayName("addContentToPlaylist 성공 - 정상 요청이면 PlaylistContent가 저장된다")
    void addContentToPlaylistSuccess() {
        // given
        Playlist playlist = mock(Playlist.class);
        User owner = mock(User.class);
        Content content = mock(Content.class);

        given(playlist.getOwner()).willReturn(owner);
        given(owner.getId()).willReturn(OWNER_ID);

        given(playlistRepository.findByIdAndDeletedAtIsNull(PLAYLIST_ID))
                .willReturn(Optional.of(playlist));
        given(contentRepository.findById(CONTENT_ID))
                .willReturn(Optional.of(content));
        given(playlistContentRepository.existsByPlaylist_IdAndContent_Id(PLAYLIST_ID, CONTENT_ID))
                .willReturn(false);

        // when
        basicPlaylistContentService.addContentToPlaylist(PLAYLIST_ID, CONTENT_ID, OWNER_ID);

        // then
        then(playlistContentRepository).should().save(any(PlaylistContent.class));
    }

    @Test
    @DisplayName("addContentToPlaylist 실패 - 플레이리스트가 존재하지 않으면 PlaylistNotFoundException이 발생한다")
    void addContentToPlaylistFailWithPlaylistNotFound() {
        // given
        given(playlistRepository.findByIdAndDeletedAtIsNull(PLAYLIST_ID))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                basicPlaylistContentService.addContentToPlaylist(PLAYLIST_ID, CONTENT_ID, OWNER_ID)
        )
                .isInstanceOf(PlaylistNotFoundException.class);

        then(contentRepository).shouldHaveNoInteractions();
        then(playlistContentRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("addContentToPlaylist 실패 - 소유자가 아니면 PlaylistAccessDeniedException이 발생한다")
    void addContentToPlaylistFailWithNotOwner() {
        // given
        Playlist playlist = mock(Playlist.class);
        User owner = mock(User.class);

        given(playlist.getOwner()).willReturn(owner);
        given(owner.getId()).willReturn(OWNER_ID);

        given(playlistRepository.findByIdAndDeletedAtIsNull(PLAYLIST_ID))
                .willReturn(Optional.of(playlist));

        // when & then
        assertThatThrownBy(() ->
                basicPlaylistContentService.addContentToPlaylist(PLAYLIST_ID, CONTENT_ID, OTHER_USER_ID)
        )
                .isInstanceOf(PlaylistAccessDeniedException.class);

        // 소유자 검증에서 막히므로 아래는 호출되면 안 됨
        then(contentRepository).shouldHaveNoInteractions();
        then(playlistContentRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("addContentToPlaylist 실패 - 콘텐츠가 존재하지 않으면 ContentNotFoundException이 발생한다")
    void addContentToPlaylistFailWithContentNotFound() {
        // given
        Playlist playlist = mock(Playlist.class);
        User owner = mock(User.class);

        given(playlist.getOwner()).willReturn(owner);
        given(owner.getId()).willReturn(OWNER_ID);

        given(playlistRepository.findByIdAndDeletedAtIsNull(PLAYLIST_ID))
                .willReturn(Optional.of(playlist));

        given(contentRepository.findById(CONTENT_ID))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                basicPlaylistContentService.addContentToPlaylist(PLAYLIST_ID, CONTENT_ID, OWNER_ID)
        )
                .isInstanceOf(ContentNotFoundException.class);

        then(playlistContentRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("addContentToPlaylist 실패 - 이미 같은 콘텐츠가 존재하면 PlaylistContentAlreadyExistsException이 발생한다")
    void addContentToPlaylistFailWithAlreadyExists() {
        // given
        Playlist playlist = mock(Playlist.class);
        User owner = mock(User.class);
        Content content = mock(Content.class);

        given(playlist.getOwner()).willReturn(owner);
        given(owner.getId()).willReturn(OWNER_ID);

        given(playlistRepository.findByIdAndDeletedAtIsNull(PLAYLIST_ID))
                .willReturn(Optional.of(playlist));
        given(contentRepository.findById(CONTENT_ID))
                .willReturn(Optional.of(content));
        given(playlistContentRepository.existsByPlaylist_IdAndContent_Id(PLAYLIST_ID, CONTENT_ID))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() ->
                basicPlaylistContentService.addContentToPlaylist(PLAYLIST_ID, CONTENT_ID, OWNER_ID)
        )
                .isInstanceOf(PlaylistContentAlreadyExistsException.class);

        then(playlistContentRepository).should(never()).save(any(PlaylistContent.class));
    }

    @Test
    @DisplayName("removeContentFromPlaylist 성공 - 소유자가 정상적으로 콘텐츠를 삭제하면 delete가 호출된다")
    void removeContentFromPlaylistSuccess() {
        // given
        User owner = createUserWithId(OWNER_ID);
        Playlist playlist = createPlaylist(owner);
        Content content = createContent("쿵푸팬더");


        PlaylistContent playlistContent = createPlaylistContent(playlist, content);

        given(playlistRepository.findByIdAndDeletedAtIsNull(PLAYLIST_ID))
                .willReturn(Optional.of(playlist));
        given(playlistContentRepository.findByPlaylist_IdAndContent_Id(PLAYLIST_ID, CONTENT_ID))
                .willReturn(Optional.of(playlistContent));

        // when
        basicPlaylistContentService.removeContentFromPlaylist(PLAYLIST_ID, CONTENT_ID, OWNER_ID);

        // then
        then(playlistRepository).should().findByIdAndDeletedAtIsNull(PLAYLIST_ID);
        then(playlistContentRepository).should().findByPlaylist_IdAndContent_Id(PLAYLIST_ID, CONTENT_ID);
        then(playlistContentRepository).should().delete(playlistContent);
    }

    @Test
    @DisplayName("removeContentFromPlaylist 실패 - 플레이리스트가 없으면 PlaylistNotFoundException 발생")
    void removeContentFromPlaylistFailWhenPlaylistNotFound() {
        // given
        given(playlistRepository.findByIdAndDeletedAtIsNull(PLAYLIST_ID))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                basicPlaylistContentService.removeContentFromPlaylist(PLAYLIST_ID, CONTENT_ID, OWNER_ID)
        )
                .isInstanceOf(PlaylistNotFoundException.class);

        then(playlistRepository).should().findByIdAndDeletedAtIsNull(PLAYLIST_ID);
        then(playlistContentRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("removeContentFromPlaylist 실패 - 소유자가 아닌 사용자가 삭제를 시도하면 PlaylistAccessDeniedException 발생")
    void removeContentFromPlaylistFailWhenOwnerNotMatch() {
        // given
        User owner = createUserWithId(OWNER_ID);
        Playlist playlist = createPlaylist(owner);

        given(playlistRepository.findByIdAndDeletedAtIsNull(PLAYLIST_ID))
                .willReturn(Optional.of(playlist));

        // when & then
        assertThatThrownBy(() ->
                basicPlaylistContentService.removeContentFromPlaylist(PLAYLIST_ID, CONTENT_ID, OTHER_USER_ID)
        )
                .isInstanceOf(PlaylistAccessDeniedException.class);

        then(playlistRepository).should().findByIdAndDeletedAtIsNull(PLAYLIST_ID);
        then(playlistContentRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("removeContentFromPlaylist 실패 - 매핑이 존재하지 않으면 PlaylistContentNotFoundException 발생")
    void removeContentFromPlaylistFailWhenMappingNotFound() {
        // given
        User owner = createUserWithId(OWNER_ID);
        Playlist playlist = createPlaylist(owner);

        given(playlistRepository.findByIdAndDeletedAtIsNull(PLAYLIST_ID))
                .willReturn(Optional.of(playlist));
        given(playlistContentRepository.findByPlaylist_IdAndContent_Id(PLAYLIST_ID, CONTENT_ID))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                basicPlaylistContentService.removeContentFromPlaylist(PLAYLIST_ID, CONTENT_ID, OWNER_ID)
        )
                .isInstanceOf(PlaylistContentNotFoundException.class);

        then(playlistRepository).should().findByIdAndDeletedAtIsNull(PLAYLIST_ID);
        then(playlistContentRepository).should().findByPlaylist_IdAndContent_Id(PLAYLIST_ID, CONTENT_ID);
        then(playlistContentRepository).should(never()).delete(any());
    }

    // ==== 테스트용 엔티티 생성 헬퍼 ====
    private User createUserWithId(UUID id) {
        User user = new User("user@email.com", "password", "test-user", null, Role.USER);
        // id 필드 주입
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private PlaylistContent createPlaylistContent(Playlist playlist, Content content) {
        return new PlaylistContent(playlist, content);
    }

    private Playlist createPlaylist(User owner) {
        Playlist playlist = new Playlist(owner, "플리 제목", "플리 설명");
        return playlist;
    }

    private Content createContent(String title) {
        Content content = new Content(Type.MOVIE, title, "설명", "abc.com", 0L, 0, 0);
        return content;
    }

}
