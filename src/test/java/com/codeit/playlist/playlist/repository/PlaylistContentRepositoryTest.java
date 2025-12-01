package com.codeit.playlist.playlist.repository;

import com.codeit.playlist.domain.content.entity.Content;
import com.codeit.playlist.domain.content.entity.Type;
import com.codeit.playlist.domain.content.repository.ContentRepository;
import com.codeit.playlist.domain.playlist.entity.Playlist;
import com.codeit.playlist.domain.playlist.entity.PlaylistContent;
import com.codeit.playlist.domain.playlist.repository.PlaylistContentRepository;
import com.codeit.playlist.domain.playlist.repository.PlaylistRepository;
import com.codeit.playlist.domain.user.entity.Role;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.repository.UserRepository;
import com.codeit.playlist.global.config.JpaConfig;
import com.codeit.playlist.global.config.QuerydslConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({QuerydslConfig.class, JpaConfig.class})
public class PlaylistContentRepositoryTest {

    @Autowired
    private PlaylistContentRepository playlistContentRepository;

    @Autowired
    private PlaylistRepository playlistRepository;

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("existsByPlaylist_IdAndContent_Id 성공 - 매핑이 존재하면 true를 반환한다")
    void successWithExistsByPlaylistIdAndContentId() {
        //given
        User owner = createUser("owner@email.com");
        userRepository.save(owner);

        Playlist playlist = createPlaylist(owner,"내 플리", "테스트용");
        playlistRepository.save(playlist);

        Content content = createContent("원피스");
        contentRepository.save(content);

        PlaylistContent playlistContent = new PlaylistContent(playlist, content);
        playlistContentRepository.save(playlistContent);

        //when
        boolean exists = playlistContentRepository.existsByPlaylist_IdAndContent_Id(
                playlist.getId(), content.getId());

        //then
        assertThat(exists).isTrue();
    }
    
    @Test
    @DisplayName("existsByPlaylist_IdAndContent_Id 실패 - 어떤 매핑도 없으면 false를 반환한다")
    void failWithExistsByPlaylistIdAndContentIdNoMapping() {
        //given
        User owner = createUser("test@email.com");
        userRepository.save(owner);

        Playlist playlist = createPlaylist(owner,"내 플리", "테스트용");
        playlistRepository.save(playlist);

        Content content = createContent("test");
        contentRepository.save(content);

        //when
        boolean exists = playlistContentRepository.existsByPlaylist_IdAndContent_Id(
                playlist.getId(), content.getId());
        
        //then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("existsByPlaylist_IdAndContent_Id 실패 - 다른 콘텐츠와만 매핑되어 있으면 false를 반환한다")
    void existsByPlaylistIdAndContentIdFailWithDifferentContent() {
        // given
        User owner = createUser("owner@email.com");
        userRepository.save(owner);

        Playlist playlist = createPlaylist(owner,"내 플리", "테스트용");
        playlistRepository.save(playlist);

        Content content1 = createContent("나의 히어로 아카데미아");
        contentRepository.save(content1);

        Content content2 = createContent("명탐정 코난");   // 테스트용 다른 콘텐츠
        contentRepository.save(content2);

        // playlist - content1만 매핑
        PlaylistContent playlistContent = new PlaylistContent(playlist, content1);
        playlistContentRepository.save(playlistContent);

        // when
        boolean exists = playlistContentRepository.existsByPlaylist_IdAndContent_Id(
                playlist.getId(), content2.getId());

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("existsByPlaylist_IdAndContent_Id 실패 - 다른 플레이리스트와의 매핑만 있으면 false를 반환한다")
    void existsByPlaylistIdAndContentIdFailWithDifferentPlaylist() {
        // given
        User owner1 = createUser("owner1@email.com");
        User owner2 = createUser("owner2@email.com");
        userRepository.save(owner1);
        userRepository.save(owner2);

        Playlist playlist1 = new Playlist(owner1, "플리1", "설명1");
        Playlist playlist2 = new Playlist(owner2, "플리2", "설명2");
        playlistRepository.save(playlist1);
        playlistRepository.save(playlist2);

        Content content = createContent("쿵푸팬더");
        contentRepository.save(content);

        // playlist1 - content만 매핑
        PlaylistContent playlistContent = new PlaylistContent(playlist1, content);
        playlistContentRepository.save(playlistContent);

        // when
        boolean exists = playlistContentRepository.existsByPlaylist_IdAndContent_Id(
                playlist2.getId(), content.getId());

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("findByPlaylist_IdAndContent_Id 성공 - 매핑이 존재하면 Optional에 값이 담긴다")
    void findByPlaylistIdAndContentIdSuccess() {
        // given
        User owner = createAndSaveUser("owner@email.com");
        Playlist playlist = createAndSavePlaylist(owner, "플리 제목", "플리 설명");
        Content content = createAndSaveContent("컨텐츠 제목");

        PlaylistContent playlistContent = new PlaylistContent(playlist, content);
        playlistContentRepository.save(playlistContent);

        UUID playlistId = playlist.getId();
        UUID contentId = content.getId();

        // when
        Optional<PlaylistContent> result =
                playlistContentRepository.findByPlaylist_IdAndContent_Id(playlistId, contentId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getPlaylist().getId()).isEqualTo(playlistId);
        assertThat(result.get().getContent().getId()).isEqualTo(contentId);
    }

    @Test
    @DisplayName("findByPlaylist_IdAndContent_Id 실패 - 존재하지 않는 playlistId면 빈 Optional 반환")
    void findByPlaylistIdAndContentIdFailWhenPlaylistNotFound() {
        // given
        User owner = createAndSaveUser("owner@email.com");
        Playlist playlist = createAndSavePlaylist(owner, "플리 제목", "플리 설명");
        Content content = createAndSaveContent("컨텐츠 제목");

        PlaylistContent playlistContent = new PlaylistContent(playlist, content);
        playlistContentRepository.save(playlistContent);

        UUID wrongPlaylistId = UUID.randomUUID();
        UUID contentId = content.getId();

        // when
        Optional<PlaylistContent> result =
                playlistContentRepository.findByPlaylist_IdAndContent_Id(wrongPlaylistId, contentId);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByPlaylist_IdAndContent_Id 실패 - 존재하지 않는 contentId면 빈 Optional 반환")
    void findByPlaylistIdAndContentIdFailWhenContentNotFound() {
        // given
        User owner = createAndSaveUser("owner@email.com");
        Playlist playlist = createAndSavePlaylist(owner, "플리 제목", "플리 설명");
        Content content = createAndSaveContent("컨텐츠 제목");

        PlaylistContent playlistContent = new PlaylistContent(playlist, content);
        playlistContentRepository.save(playlistContent);

        UUID playlistId = playlist.getId();
        UUID wrongContentId = UUID.randomUUID();

        // when
        Optional<PlaylistContent> result =
                playlistContentRepository.findByPlaylist_IdAndContent_Id(playlistId, wrongContentId);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByPlaylist_IdAndContent_Id 실패 - playlist와 content가 모두 존재하지 않으면 빈 Optional 반환")
    void findByPlaylistIdAndContentIdFailWhenPlaylistAndContentNotFound() {
        // given
        UUID wrongPlaylistId = UUID.randomUUID();
        UUID wrongContentId = UUID.randomUUID();

        // when
        Optional<PlaylistContent> result =
                playlistContentRepository.findByPlaylist_IdAndContent_Id(wrongPlaylistId, wrongContentId);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByPlaylist_IdAndContent_Id 실패 - playlist와 content는 존재하지만 매핑이 없으면 빈 Optional 반환")
    void findByPlaylistIdAndContentIdFailWhenMappingNotExists() {
        // given
        User owner = createAndSaveUser("owner@email.com");
        Playlist playlist = createAndSavePlaylist(owner, "플리 제목", "플리 설명");
        Content content1 = createAndSaveContent("컨텐츠 제목1");
        Content content2 = createAndSaveContent("컨텐츠 제목2");

        // playlist - content1 매핑만 저장
        PlaylistContent playlistContent = new PlaylistContent(playlist, content1);
        playlistContentRepository.save(playlistContent);

        UUID playlistId = playlist.getId();
        UUID notMappedContentId = content2.getId();

        // when
        Optional<PlaylistContent> result =
                playlistContentRepository.findByPlaylist_IdAndContent_Id(playlistId, notMappedContentId);

        // then
        assertThat(result).isEmpty();
    }

    // ==== 테스트용 엔티티 생성 헬퍼 ====
    private User createUser(String email) {
        User user = new User(email, "password", "test-user", null, Role.USER);
        return user;
    }

    private User createAndSaveUser(String email) {
        User user = createUser(email);
        return userRepository.save(user);
    }

    private Content createContent(String title) {
        Content content = new Content(Type.MOVIE, title, "설명", "abc.com", 0L, 0, 0);
        return content;
    }

    private Content createAndSaveContent(String title) {
        Content content = createContent(title);
        return contentRepository.save(content);
    }
    
    private Playlist createPlaylist(User owner, String title, String description) {
        Playlist playlist = new Playlist(owner, title, description);
        return playlist;
    }

    private Playlist createAndSavePlaylist(User owner, String title, String description) {
        Playlist playlist = createPlaylist(owner, title, description);
        return playlistRepository.save(playlist);
    }
}
