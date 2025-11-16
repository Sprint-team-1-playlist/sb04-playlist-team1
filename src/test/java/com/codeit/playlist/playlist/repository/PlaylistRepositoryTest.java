package com.codeit.playlist.playlist.repository;

import com.codeit.playlist.domain.config.QuerydslConfig;
import com.codeit.playlist.domain.playlist.entity.Playlist;
import com.codeit.playlist.domain.playlist.entity.Subscribe;
import com.codeit.playlist.domain.playlist.repository.PlaylistRepository;
import com.codeit.playlist.domain.playlist.repository.SubscribeRepository;
import com.codeit.playlist.domain.user.entity.Role;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.repository.UserRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QuerydslConfig.class)
public class PlaylistRepositoryTest {

    @Autowired
    PlaylistRepository playlistRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    SubscribeRepository subscribeRepository;

    @Autowired
    private JPAQueryFactory queryFactory;

    @Test
    @DisplayName("searchPlaylists 성공 - 구독자 필터로 자신이 구독한 플레이리스트만 조회")
    void searchPlaylistsSuccessWithSubscriberFilter() {
        // given
        User user1 = createTestUser("user1@email.com");
        User user2 = createTestUser("user2@email.com");
        userRepository.saveAll(List.of(user1, user2));

        Playlist p1 = createPlaylist(user1, "플리1");
        Playlist p2 = createPlaylist(user1, "플리2");
        Playlist p3 = createPlaylist(user2, "플리3");
        playlistRepository.saveAll(List.of(p1, p2, p3));

        // user1 이 p1, p3 를 구독
        subscribeRepository.save(new Subscribe(user1, p1));
        subscribeRepository.save(new Subscribe(user1, p3));

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Slice<Playlist> slice = playlistRepository.searchPlaylists(
                null,                // keywordLike
                null,                // ownerIdEqual
                user1.getId(),       // subscriberIdEqual
                false,               // hasCursor
                null,                // cursorId
                true,                // asc
                "updatedAt",
                pageable
        );

        // then
        List<Playlist> content = slice.getContent();
        assertThat(content).extracting(Playlist::getId)
                .containsExactlyInAnyOrder(p1.getId(), p3.getId());
        assertThat(slice.hasNext()).isFalse();
    }

    @Test
    @DisplayName("searchPlaylists 실패 케이스 - 구독자가 아무 것도 구독하지 않으면 빈 결과 반환")
    void searchPlaylistsNoSubscriptionsReturnsEmpty() {
        // given
        User user = createTestUser("no-sub@email.com");
        userRepository.save(user);

        // 플레이리스트만 존재
        Playlist p1 = createPlaylist(user, "플리1");
        playlistRepository.save(p1);

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Slice<Playlist> slice = playlistRepository.searchPlaylists(
                null,
                null,
                user.getId(),  // 구독자 필터
                false,
                null,
                true,
                "updatedAt",
                pageable
        );

        // then
        assertThat(slice.getContent()).isEmpty();
        assertThat(slice.hasNext()).isFalse();
    }

    // ==== 테스트용 엔티티 생성 헬퍼 메서드 ====

    private Playlist createPlaylist(User owner, String title) {
        Playlist playlist = new Playlist(owner, title, "설명입니다", 0L);
        return playlist;
    }

    public static User createTestUser(String email) {
        return new User(email, "password", "test-user", null, Role.USER);
    }
}
