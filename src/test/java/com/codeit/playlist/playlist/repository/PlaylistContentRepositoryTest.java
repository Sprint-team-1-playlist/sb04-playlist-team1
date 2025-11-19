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
import com.codeit.playlist.global.config.QuerydslConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QuerydslConfig.class)
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

        Playlist playlist = createPlaylist(owner);
        playlistRepository.save(playlist);

        Content content = createContent();
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

        Playlist playlist = createPlaylist(owner);
        playlistRepository.save(playlist);

        Content content = createContent();
        contentRepository.save(content);

        //when
        boolean exists = playlistContentRepository.existsByPlaylist_IdAndContent_Id(
                playlist.getId(), content.getId());
        
        //then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("existsByPlaylist_IdAndContent_Id 실패 - 다른 콘텐츠와만 매핑되어 있으면 false를 반환한다")
    void existsByPlaylistIdAndContentId_fail_differentContent() {
        // given
        User owner = createUser("owner@email.com");
        userRepository.save(owner);

        Playlist playlist = createPlaylist(owner);
        playlistRepository.save(playlist);

        Content content1 = createContent();
        contentRepository.save(content1);

        Content content2 = createContent();   // 테스트용 다른 콘텐츠
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
    void existsByPlaylistIdAndContentId_fail_differentPlaylist() {
        // given
        User owner1 = createUser("owner1@email.com");
        User owner2 = createUser("owner2@email.com");
        userRepository.save(owner1);
        userRepository.save(owner2);

        Playlist playlist1 = new Playlist(owner1, "플리1", "설명1", 0L, new ArrayList<>());
        Playlist playlist2 = new Playlist(owner2, "플리2", "설명2", 0L, new ArrayList<>());
        playlistRepository.save(playlist1);
        playlistRepository.save(playlist2);

        Content content = createContent();
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

    // ==== 테스트용 엔티티 생성 헬퍼 ====
    private User createUser(String email) {
        User user = new User(email, "password", "test-user", null, Role.USER);
        return user;
    }

    private Content createContent() {
        Content content = new Content(Type.MOVIE,"title", "설명", "abc.com", 0L, 0, 0);
        return content;
    }
    
    private Playlist createPlaylist(User owner) {
        Playlist playlist = new Playlist(owner, "플리 제목", "플리 설명", 0L, new ArrayList<>());
        return playlist;
    }
}
