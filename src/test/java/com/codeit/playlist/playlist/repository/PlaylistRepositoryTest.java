package com.codeit.playlist.playlist.repository;

import com.codeit.playlist.domain.config.QuerydslConfig;
import com.codeit.playlist.domain.playlist.entity.Playlist;
import com.codeit.playlist.domain.playlist.entity.Subscribe;
import com.codeit.playlist.domain.playlist.repository.PlaylistRepository;
import com.codeit.playlist.domain.playlist.repository.SubscribeRepository;
import com.codeit.playlist.domain.user.entity.Role;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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
    private TestEntityManager entityManager;

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
    @DisplayName("searchPlaylists 성공 - 수정 날짜 내림차순 정렬 확인")
    void searchPlaylistsSortedByUpdatedAtDesc() {
        // given
        User owner = createTestUser("owner@email.com");
        userRepository.save(owner);

        Playlist p1 = createPlaylist(owner, "플리1");
        Playlist p2 = createPlaylist(owner, "플리2");
        Playlist p3 = createPlaylist(owner, "플리3");

        // updatedAt 강제 조정
        ReflectionTestUtils.setField(p1, "updatedAt", LocalDateTime.now().minusHours(3));
        ReflectionTestUtils.setField(p2, "updatedAt", LocalDateTime.now().minusHours(2));
        ReflectionTestUtils.setField(p3, "updatedAt", LocalDateTime.now().minusHours(1));

        playlistRepository.saveAll(List.of(p1, p2, p3));

        Pageable pageable = PageRequest.of(0, 10);

        // when - sortBy를 꼭 전달해야 함
        Slice<Playlist> slice = playlistRepository.searchPlaylists(
                null, null, null,
                false, null,
                false,                 // DESC
                "updatedAt",           //정렬 기준
                pageable
        );

        // then
        List<Playlist> content = slice.getContent();

        assertThat(content.get(0).getUpdatedAt())
                .isAfter(content.get(1).getUpdatedAt());
        assertThat(content.get(1).getUpdatedAt())
                .isAfter(content.get(2).getUpdatedAt());
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

    @Test
    @DisplayName("논리삭제 - deletedAt이 null에서 현재 시간으로 업데이트된다.")
    void successWithSoftDeletedById() {
        //given
        User owner = createTestUser("test@email.com");
        entityManager.persist(owner);

        Playlist playlist = new Playlist(owner, "제목", "설명", 0L);
        entityManager.persist(playlist);
        entityManager.flush();
        entityManager.clear();

        UUID playlistId = playlist.getId();

        //when
        int updatedCount = playlistRepository.softDeleteById(playlistId);

        //then
        assertThat(updatedCount).isEqualTo(1);

        Playlist deleted = entityManager.find(Playlist.class, playlistId);
        assertThat(deleted.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("논리삭제 - 이미 삭제되어 deletedAt이 not null인 경우 0을 반환한다.")
    void failWithsoftDeleteByIdAlreadyDeleted() {
        //given
        User owner = createTestUser("testmail@test.com");
        entityManager.persist(owner);

        Playlist playlist = new Playlist(owner, "제목", "설명", 0L);
        playlist.setDeletedAt(LocalDateTime.now());
        entityManager.persist(playlist);
        entityManager.flush();
        entityManager.clear();

        UUID playlistId = playlist.getId();

        //when
        int updatedCount = playlistRepository.softDeleteById(playlistId);

        //then
        assertThat(updatedCount).isEqualTo(0);
    }

    @Test
    @DisplayName("findAllDeletedBefore - threshold 이전에 soft delete 된 데이터만 조회된다")
    void findAllDeletedBeforeSuccess() {
        // given
        User owner = createTestUser("testmail@test.com");
        entityManager.persist(owner);

        LocalDateTime now = LocalDateTime.now();

        Playlist oldDeleted = new Playlist(owner, "old", "old desc", 0L);
        oldDeleted.setDeletedAt(now.minusDays(8)); // 7일보다 더 이전

        Playlist recentDeleted = new Playlist(owner, "recent", "desc", 0L);
        recentDeleted.setDeletedAt(now.minusDays(3)); // 7일 이전이 아님

        Playlist notDeleted = new Playlist(owner, "notDeleted", "desc", 0L);

        entityManager.persist(oldDeleted);
        entityManager.persist(recentDeleted);
        entityManager.persist(notDeleted);
        entityManager.flush();
        entityManager.clear();

        LocalDateTime threshold = now.minusDays(7);

        // when
        List<Playlist> result = playlistRepository.findAllDeletedBefore(threshold);

        // then
        assertThat(result)
                .extracting(Playlist::getTitle)
                .containsExactly("old");
    }

    @Test
    @DisplayName("findAllDeletedBefore - 삭제된 데이터가 없으면 빈 리스트를 반환한다")
    void findAllDeletedBeforeNoDeletedData() {
        // given
        User owner = createTestUser("test@mail.com");
        entityManager.persist(owner);

        Playlist playlist = new Playlist(owner, "normal", "desc", 0L);
        entityManager.persist(playlist);

        entityManager.flush();
        entityManager.clear();

        LocalDateTime threshold = LocalDateTime.now().minusDays(7);

        // when
        List<Playlist> result = playlistRepository.findAllDeletedBefore(threshold);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAllDeletedBefore - 삭제된지 7일이 지나지 않은 경우 결과에 포함되지 않는다")
    void findAllDeletedBeforeNotOldEnough() {
        // given
        User owner = createTestUser("email@test.com");
        entityManager.persist(owner);

        Playlist playlist = new Playlist(owner, "recent", "desc", 0L);
        entityManager.persist(playlist);

        // 👉 deletedAt 강제 세팅 (ReflectionTestUtils 사용)
        ReflectionTestUtils.setField(playlist, "deletedAt",
                LocalDateTime.now().minusDays(3)); // 7일 이전 X

        entityManager.flush();
        entityManager.clear();

        LocalDateTime threshold = LocalDateTime.now().minusDays(7);

        // when
        List<Playlist> result = playlistRepository.findAllDeletedBefore(threshold);

        // then
        assertThat(result).isEmpty();
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
