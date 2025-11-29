package com.codeit.playlist.playlist.repository;

import com.codeit.playlist.domain.playlist.entity.Playlist;
import com.codeit.playlist.domain.playlist.entity.Subscribe;
import com.codeit.playlist.domain.playlist.repository.PlaylistRepository;
import com.codeit.playlist.domain.playlist.repository.SubscribeRepository;
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

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({QuerydslConfig.class, JpaConfig.class})
public class SubscribeRepositoryTest {

    @Autowired
    SubscribeRepository subscribeRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PlaylistRepository playlistRepository;

    @Test
    @DisplayName("existsBySubscriberAndPlaylist - 구독이 존재하면 true를 반환한다")
    void existsBySubscriberAndPlaylistTrue() {
        // given
        User subscriber = userRepository.save(createUser("testmail@mail.com"));
        Playlist playlist = playlistRepository.save(createPlaylist(subscriber));

        subscribeRepository.save(new Subscribe(subscriber, playlist));

        // when
        boolean exists = subscribeRepository.existsBySubscriberAndPlaylist(subscriber, playlist);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsBySubscriberAndPlaylist - 구독이 존재하지 않으면 false를 반환한다")
    void existsBySubscriberAndPlaylistFalse() {
        // given
        User subscriber = userRepository.save(createUser("testmail@mail.com"));
        Playlist playlist = playlistRepository.save(createPlaylist(subscriber));

        // when
        boolean exists = subscribeRepository.existsBySubscriberAndPlaylist(subscriber, playlist);

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("findBySubscriberAndPlaylist - 구독이 존재하면 Subscribe를 반환한다")
    void findBySubscriberAndPlaylistSuccess() {
        // given
        User subscriber = userRepository.save(createUser("testmail@mail.com"));
        Playlist playlist = playlistRepository.save(createPlaylist(subscriber));
        Subscribe saved = subscribeRepository.save(new Subscribe(subscriber, playlist));

        // when
        Optional<Subscribe> result =
                subscribeRepository.findBySubscriberAndPlaylist(subscriber, playlist);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    @DisplayName("findBySubscriberAndPlaylist - 구독이 존재하지 않으면 빈 Optional을 반환한다")
    void findBySubscriberAndPlaylistEmpty() {
        // given
        User subscriber = userRepository.save(createUser("testmail@mail.com"));
        Playlist playlist = playlistRepository.save(createPlaylist(subscriber));

        // when
        Optional<Subscribe> result =
                subscribeRepository.findBySubscriberAndPlaylist(subscriber, playlist);

        // then
        assertThat(result).isEmpty();
    }

    // ====== 테스트용 엔티티 생성 메서드 ======
    public static User createUser(String email) {
        return new User(email, "password", "test-user", null, Role.USER);
    }

    private Playlist createPlaylist(User owner) {
        return new Playlist(owner, "테스트 플레이리스트", "테스트 설명");
    }
}
