package com.codeit.playlist.playlist.repository;

import com.codeit.playlist.global.config.QuerydslConfig;
import com.codeit.playlist.domain.content.entity.Content;
import com.codeit.playlist.domain.content.entity.Type;
import com.codeit.playlist.domain.content.repository.ContentRepository;
import com.codeit.playlist.domain.playlist.entity.Playlist;
import com.codeit.playlist.domain.playlist.entity.PlaylistContent;
import com.codeit.playlist.domain.playlist.entity.Subscribe;
import com.codeit.playlist.domain.playlist.repository.PlaylistRepository;
import com.codeit.playlist.domain.playlist.repository.SubscribeRepository;
import com.codeit.playlist.domain.user.entity.Role;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.repository.UserRepository;
import org.hibernate.Hibernate;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    @Autowired
    private ContentRepository contentRepository;

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

        Playlist playlist = new Playlist(owner, "제목", "설명", 0L, new java.util.ArrayList<>());
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

        Playlist playlist = new Playlist(owner, "제목", "설명", 0L, new java.util.ArrayList<>());
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

        Playlist oldDeleted = new Playlist(owner, "old", "old desc", 0L, new java.util.ArrayList<>());
        oldDeleted.setDeletedAt(now.minusDays(8)); // 7일보다 더 이전

        Playlist recentDeleted = new Playlist(owner, "recent", "desc", 0L, new java.util.ArrayList<>());
        recentDeleted.setDeletedAt(now.minusDays(3)); // 7일 이전이 아님

        Playlist notDeleted = new Playlist(owner, "notDeleted", "desc", 0L, new java.util.ArrayList<>());

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

        Playlist playlist = new Playlist(owner, "normal", "desc", 0L, new java.util.ArrayList<>());
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

        Playlist playlist = new Playlist(owner, "recent", "desc", 0L, new java.util.ArrayList<>());
        entityManager.persist(playlist);

        // deletedAt 강제 세팅 (ReflectionTestUtils 사용)
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

    @Test
    @DisplayName("findWithDetailsById로 플레이리스트와 소유자, 콘텐츠를 함께 조회한다.")
    void successWithFindWithDetailsById() {
        //given
        User owner = createTestUser("test@mail.com");
        userRepository.save(owner);

        Playlist playlist = new Playlist(owner, "테스트 플리", "테스트용 설명", 0L, new java.util.ArrayList<>());
        playlistRepository.save(playlist);

        Content content1 = createTestContent("콘텐츠1");
        Content content2 = createTestContent("content2");
        contentRepository.saveAll(List.of(content1, content2));

        playlist.addContent(content1);
        playlist.addContent(content2);

        playlistRepository.save(playlist);

        entityManager.flush();
        entityManager.clear();

        //when
        Optional<Playlist> opt = playlistRepository.findWithDetailsById(playlist.getId());

        //then
        assertThat(opt).isPresent();
        Playlist found = opt.get();

        // (1) 플레이리스트 기본 필드 검증
        assertThat(found.getTitle()).isEqualTo("테스트 플리");
        assertThat(found.getDescription()).isEqualTo("테스트용 설명");

        // (2) 소유자 검증
        assertThat(found.getOwner()).isNotNull();
        assertThat(found.getOwner().getEmail()).isEqualTo(owner.getEmail());

        // (3) 플레이리스트 안의 콘텐츠(PlaylistContent) 검증
        List<PlaylistContent> playlistContents = found.getPlaylistContents();
        assertThat(playlistContents).hasSize(2);

        List<String> contentTitles = playlistContents.stream()
                .map(pc -> pc.getContent().getTitle())
                .toList();

        assertThat(contentTitles)
                .containsExactlyInAnyOrder("콘텐츠1", "content2");

        assertThat(Hibernate.isInitialized(found.getOwner())).isTrue();
        assertThat(Hibernate.isInitialized(found.getPlaylistContents())).isTrue();
        assertThat(Hibernate.isInitialized(found.getPlaylistContents().get(0).getContent())).isTrue();
    }

    @Test
    @DisplayName("findWithDetailsById 실패 - 존재하지 않는 ID면 빈 Optional을 반환한다.")
    void failWithFindWithDetailsByIdNotFound() {
        // given
        User owner = createTestUser("test@mail.com");
        userRepository.save(owner);

        Playlist playlist = new Playlist(owner, "플리", "설명", 0L, new ArrayList<>());
        playlistRepository.save(playlist);

        // 실제로 저장되지 않은 랜덤 ID
        UUID notExistId = UUID.randomUUID();

        // when
        Optional<Playlist> opt = playlistRepository.findWithDetailsById(notExistId);

        // then
        assertThat(opt).isEmpty();
    }

    @Test
    @DisplayName("findWithDetailsById 실패 - 논리삭제된 플레이리스트는 조회되지 않는다.")
    void failWithFindWithDetailsByIdSoftDeleted() {
        // given
        User owner = createTestUser("delete@test.com");
        userRepository.save(owner);

        Playlist playlist = new Playlist(owner, "삭제될 플리", "설명", 0L, new ArrayList<>());
        playlistRepository.save(playlist);

        // 논리 삭제
        playlist.setDeletedAt(LocalDateTime.now());
        playlistRepository.save(playlist);

        entityManager.flush();
        entityManager.clear();

        // when
        Optional<Playlist> opt = playlistRepository.findWithDetailsById(playlist.getId());

        // then
        assertThat(opt).isEmpty();
    }

    @Test
    @DisplayName("increaseSubscriberCount 성공 - 해당 플레이리스트의 구독자 수가 1 증가한다")
    void increaseSubscriberCountSuccess() {
        // given
        Playlist playlist = createPlaylistWithSubscriberCount(3L); // 초기값 3
        UUID playlistId = playlist.getId();

        // when
        int updatedRows = playlistRepository.increaseSubscriberCount(playlistId);

        // then
        assertThat(updatedRows).isEqualTo(1);

        Playlist reloaded = playlistRepository.findById(playlistId)
                .orElseThrow();
        assertThat(reloaded.getSubscriberCount()).isEqualTo(4L);
    }

    @Test
    @DisplayName("increaseSubscriberCount 실패 - 존재하지 않는 플레이리스트 ID면 업데이트되지 않는다")
    void increaseSubscriberCountFailWhenPlaylistNotFound() {
        // given
        UUID notExistingId = UUID.randomUUID();

        // when
        int updatedRows = playlistRepository.increaseSubscriberCount(notExistingId);

        // then
        assertThat(updatedRows).isEqualTo(0);
    }

    @Test
    @DisplayName("decreaseSubscriberCount 성공 - 구독자 수가 1 이상일 때 1 감소한다")
    void decreaseSubscriberCountSuccess() {
        // given
        Playlist playlist = createPlaylistWithSubscriberCount(2L); // 초기값 2
        UUID playlistId = playlist.getId();

        // when
        int updatedRows = playlistRepository.decreaseSubscriberCount(playlistId);

        // then
        assertThat(updatedRows).isEqualTo(1);

        Playlist reloaded = playlistRepository.findById(playlistId)
                .orElseThrow();
        assertThat(reloaded.getSubscriberCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("decreaseSubscriberCount 실패 - 구독자 수가 0이면 감소하지 않는다")
    void decreaseSubscriberCountFailWhenZero() {
        // given
        Playlist playlist = createPlaylistWithSubscriberCount(0L); // 초기값 0
        UUID playlistId = playlist.getId();

        // when
        int updatedRows = playlistRepository.decreaseSubscriberCount(playlistId);

        // then
        assertThat(updatedRows).isEqualTo(0);

        Playlist reloaded = playlistRepository.findById(playlistId)
                .orElseThrow();
        assertThat(reloaded.getSubscriberCount()).isEqualTo(0L);
    }

    // ==== 테스트용 엔티티 생성 헬퍼 메서드 ====

    private Playlist createPlaylistWithSubscriberCount(long subscriberCount) {

        User owner = createTestUser("owner@test.com");
        owner = userRepository.save(owner);

        Playlist playlist = createPlaylist(owner, "테스트 제목");

        ReflectionTestUtils.setField(playlist, "subscriberCount", subscriberCount);

        return playlistRepository.saveAndFlush(playlist);
    }

    private Playlist createPlaylist(User owner, String title) {
        Playlist playlist = new Playlist(owner, title, "설명입니다", 0L, null);
        return playlist;
    }

    public static User createTestUser(String email) {
        return new User(email, "password", "test-user", null, Role.USER);
    }

    public static Content createTestContent(String title) {
        return new Content(Type.MOVIE, title, "설명", "abc.com", 0L, 0, 0);
    }
}
