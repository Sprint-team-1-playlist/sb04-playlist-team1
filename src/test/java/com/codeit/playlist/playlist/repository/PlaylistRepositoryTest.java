package com.codeit.playlist.playlist.repository;

import com.codeit.playlist.domain.content.entity.Content;
import com.codeit.playlist.domain.content.repository.ContentRepository;
import com.codeit.playlist.domain.playlist.entity.Playlist;
import com.codeit.playlist.domain.playlist.entity.PlaylistContent;
import com.codeit.playlist.domain.playlist.entity.Subscribe;
import com.codeit.playlist.domain.playlist.repository.PlaylistRepository;
import com.codeit.playlist.domain.playlist.repository.SubscribeRepository;
import com.codeit.playlist.domain.playlist.repository.custom.PlaylistRepositoryCustomImpl;
import com.codeit.playlist.domain.user.entity.Role;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.repository.UserRepository;
import com.codeit.playlist.global.config.JpaConfig;
import com.codeit.playlist.global.config.QuerydslConfig;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({QuerydslConfig.class, JpaConfig.class})
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

    @Autowired
    JPAQueryFactory queryFactory;

    private PlaylistRepositoryCustomImpl customRepository;

    private static final AtomicLong TMDB_ID_SEQ = new AtomicLong(1);

    @BeforeEach
    void initCustom() {
        customRepository = new PlaylistRepositoryCustomImpl(queryFactory);
    }

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

        playlistRepository.saveAll(List.of(p1, p2, p3));
        entityManager.flush();

        EntityManager em = entityManager.getEntityManager();

        // updatedAt 강제 조정(Auditing 우회)
        Instant base = Instant.now();
        em.createQuery("UPDATE Playlist p SET p.updatedAt = :t WHERE p.id = :id")
                .setParameter("t", base.minus(3, ChronoUnit.HOURS))
                .setParameter("id", p1.getId())
                .executeUpdate();

        em.createQuery("UPDATE Playlist p SET p.updatedAt = :t WHERE p.id = :id")
                .setParameter("t", base.minus(2, ChronoUnit.HOURS))
                .setParameter("id", p2.getId())
                .executeUpdate();

        em.createQuery("UPDATE Playlist p SET p.updatedAt = :t WHERE p.id = :id")
                .setParameter("t", base.minus(1, ChronoUnit.HOURS))
                .setParameter("id", p3.getId())
                .executeUpdate();

        entityManager.clear();

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
    @DisplayName("countPlaylists 성공 - keyword와 ownerId 조건이 정상 적용된다")
    void countPlaylistsSuccessWithKeywordAndOwnerFilter() {
        // given
        User owner1 = userRepository.save(createTestUser("owner1@test.com"));
        User owner2 = userRepository.save(createTestUser("owner2@test.com"));

        Playlist p1 = new Playlist(owner1, "테스트 플리 1", "설명입니다");
        Playlist p2 = new Playlist(owner1, "테스트 플리 2", "테스트 설명");
        Playlist p3 = new Playlist(owner1, "다른 제목", "설명입니다");

        Playlist p4 = new Playlist(owner2, "테스트 플리 3", "설명입니다");

        playlistRepository.saveAll(List.of(p1, p2, p3, p4));

        String keywordLike = "테스트";
        UUID ownerIdEqual = owner1.getId();
        UUID subscriberIdEqual = null;

        // when
        long count = customRepository.countPlaylists(keywordLike, ownerIdEqual, subscriberIdEqual);

        // then
        assertThat(count).isEqualTo(2L);
    }

    @Test
    @DisplayName("countPlaylists 경계 - 조건에 맞는 플레이리스트가 없으면 0을 반환한다")
    void countPlaylistsReturnsZeroWhenNoMatch() {
        // given
        User owner = userRepository.save(createTestUser("owner@test.com"));

        // 키워드와 상관없는 플레이리스트 하나
        Playlist p = new Playlist(owner, "아무 제목", "아무 설명");
        playlistRepository.save(p);

        String keywordLike = "없는키워드";   // 매칭 안 되는 키워드
        UUID ownerIdEqual = owner.getId();
        UUID subscriberIdEqual = null;

        // when
        long count = customRepository.countPlaylists(keywordLike, ownerIdEqual, subscriberIdEqual);

        // then
        assertThat(count).isEqualTo(0L);
    }

    @Test
    @DisplayName("논리삭제 - deletedAt이 null에서 현재 시간으로 업데이트된다.")
    void successWithSoftDeletedById() {
        //given
        User owner = createTestUser("test@email.com");
        entityManager.persist(owner);

        Playlist playlist = new Playlist(owner, "제목", "설명");
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
    void failWithSoftDeleteByIdAlreadyDeleted() {
        //given
        User owner = createTestUser("testmail@test.com");
        entityManager.persist(owner);

        Playlist playlist = new Playlist(owner, "제목", "설명");
        playlist.setDeletedAt(Instant.now());
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

        Instant now = Instant.now();

        Playlist oldDeleted = new Playlist(owner, "old", "old desc");
        oldDeleted.setDeletedAt(now.minus(8,ChronoUnit.DAYS)); // 7일보다 더 이전

        Playlist recentDeleted = new Playlist(owner, "recent", "desc");
        recentDeleted.setDeletedAt(now.minus(3, ChronoUnit.DAYS)); // 7일 이전이 아님

        Playlist notDeleted = new Playlist(owner, "notDeleted", "desc");

        entityManager.persist(oldDeleted);
        entityManager.persist(recentDeleted);
        entityManager.persist(notDeleted);
        entityManager.flush();
        entityManager.clear();

        Instant threshold = now.minus(7, ChronoUnit.DAYS);

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

        Playlist playlist = new Playlist(owner, "normal", "desc");
        entityManager.persist(playlist);

        entityManager.flush();
        entityManager.clear();

        Instant threshold = Instant.now().minus(7, ChronoUnit.DAYS);

        // when
        List<Playlist> result = playlistRepository.findAllDeletedBefore(threshold);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAllDeletedBefore - 삭제된지 7일이 지나지 않은 경우 결과에 포함되지 않는다")
    void findAllDeletedBeforeNotOldEnough() {
        // given
        User saved = createTestUser("email@test.com");
        User owner = userRepository.save(saved);

        Playlist playlistSaved = new Playlist(owner, "recent", "desc");
        Playlist playlist = playlistRepository.save(playlistSaved);

        EntityManager em = entityManager.getEntityManager();

        Instant deletedAt = Instant.now().minus(3, ChronoUnit.DAYS);

        // deletedAt 강제 세팅
        em.createQuery("UPDATE Playlist p SET p.deletedAt = :t WHERE p.id = :id")
                .setParameter("t", deletedAt)
                .setParameter("id", playlist.getId())
                .executeUpdate();

        entityManager.flush();
        entityManager.clear();

        Instant threshold = Instant.now().minus(7, ChronoUnit.DAYS);

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

        Playlist playlist = new Playlist(owner, "테스트 플리", "테스트용 설명");
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

        Playlist playlist = new Playlist(owner, "플리", "설명");
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

        Playlist playlist = new Playlist(owner, "삭제될 플리", "설명");
        playlistRepository.save(playlist);

        // 논리 삭제
        playlist.setDeletedAt(Instant.now());
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

    @Test
    @DisplayName("searchPlaylists 성공 - updatedAt DESC 기준 커서 이후 데이터만 조회된다")
    void searchPlaylistsCursorUpdatedAtDesc() {
        // given
        User owner = userRepository.save(createTestUser("owner@test.com"));

        Playlist p1 = playlistRepository.save(new Playlist(owner, "플리1", "설명"));
        Playlist p2 = playlistRepository.save(new Playlist(owner, "플리2", "설명"));
        Playlist p3 = playlistRepository.save(new Playlist(owner, "플리3", "설명"));

        Instant base = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        updatePlaylistUpdatedAt(p1.getId(), base.minus(3, ChronoUnit.DAYS));
        updatePlaylistUpdatedAt(p2.getId(), base.minus(2, ChronoUnit.DAYS));
        updatePlaylistUpdatedAt(p3.getId(), base.minus(1, ChronoUnit.DAYS));

        entityManager.flush();
        entityManager.clear();

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Slice<Playlist> slice = customRepository.searchPlaylists(
                null, owner.getId(), null,
                true, p2.getId(),
                false, // asc=false => DESC
                "updatedAt",
                pageable
        );

        // then
        assertThat(slice.getContent())
                .extracting(Playlist::getId)
                .containsExactly(p1.getId());
    }

    @Test
    @DisplayName("searchPlaylists 성공 - subscribeCount DESC 기준 커서 이후 데이터만 조회된다")
    void searchPlaylistsCursorSubscribeCountDesc() {
        // given
        User owner = userRepository.save(createTestUser("owner@test.com"));

        // 플레이리스트 저장(기본 updatedAt 등 자동 저장)
        Playlist p1 = playlistRepository.save(new Playlist(owner, "플리1", "설명"));
        Playlist p2 = playlistRepository.save(new Playlist(owner, "플리2", "설명"));
        Playlist p3 = playlistRepository.save(new Playlist(owner, "플리3", "설명"));

        updatePlaylistSubscriberCount(p1.getId(), 10L);
        updatePlaylistSubscriberCount(p2.getId(), 20L);
        updatePlaylistSubscriberCount(p3.getId(), 30L);

        entityManager.flush();
        entityManager.clear();

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Slice<Playlist> slice = customRepository.searchPlaylists(
                null, owner.getId(), null,
                true, p2.getId(),
                false,
                "subscribeCount",
                pageable
        );

        // then
        assertThat(slice.getContent())
                .extracting(Playlist::getId)
                .containsExactly(p1.getId());
    }


    @Test
    @DisplayName("searchPlaylists 실패 - hasCursor가 false이면 커서 조건이 적용되지 않는다")
    void searchPlaylistsNoCursorDoesNotFilter() {
        //given
        User owner = userRepository.save(createTestUser("owner@test.com"));

        Playlist p1 = playlistRepository.save(new Playlist(owner, "플리1", "설명"));
        Playlist p2 = playlistRepository.save(new Playlist(owner, "플리2", "설명"));
        Playlist p3 = playlistRepository.save(new Playlist(owner, "플리3", "설명"));

        Instant base = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        updatePlaylistUpdatedAt(p1.getId(), base.minus(3, ChronoUnit.DAYS));
        updatePlaylistUpdatedAt(p2.getId(), base.minus(2, ChronoUnit.DAYS));
        updatePlaylistUpdatedAt(p3.getId(), base.minus(1, ChronoUnit.DAYS));

        entityManager.flush();
        entityManager.clear();

        Pageable pageable = PageRequest.of(0, 10);

        //when
        Slice<Playlist> slice = customRepository.searchPlaylists(
                null, owner.getId(), null,
                false, p2.getId(), // hasCursor=false
                false,
                "updatedAt",
                pageable
        );

        //then
        assertThat(slice.getContent())
                .extracting(Playlist::getId)
                .containsExactly(p3.getId(), p2.getId(), p1.getId());
    }

    @Test
    @DisplayName("searchPlaylists 실패 - cursorId에 해당하는 Playlist가 없으면 커서 조건이 적용되지 않는다")
    void searchPlaylistsCursorNotFoundDoesNotFilter() {
        // given
        User owner = userRepository.save(createTestUser("owner@test.com"));

        Playlist p1 = playlistRepository.save(new Playlist(owner, "플리1", "설명"));
        Playlist p2 = playlistRepository.save(new Playlist(owner, "플리2", "설명"));

        entityManager.flush();
        entityManager.clear();

        Pageable pageable = PageRequest.of(0, 10);

        //when
        Slice<Playlist> slice = customRepository.searchPlaylists(
                null, owner.getId(), null,
                true, UUID.randomUUID(), // 없는 커서
                false,
                "updatedAt",
                pageable
        );

        //then
        assertThat(slice.getContent()).hasSize(2);
    }


    // ==== 테스트용 엔티티 생성 헬퍼 메서드 ====

    private Playlist createPlaylistWithSubscriberCount(long subscriberCount) {

        User owner = createTestUser("owner@test.com");
        owner = userRepository.save(owner);

        Playlist playlist = playlistRepository.saveAndFlush(createPlaylist(owner, "테스트 제목"));

        EntityManager em = entityManager.getEntityManager();

        em.createQuery(
                        "UPDATE Playlist p SET p.subscriberCount = :c WHERE p.id = :id")
                .setParameter("c", subscriberCount)
                .setParameter("id", playlist.getId())
                .executeUpdate();

        entityManager.flush();
        entityManager.clear();

        // 최신 값으로 다시 로딩해서 반환
        return playlistRepository.findById(playlist.getId())
                .orElseThrow();
    }

    private Playlist createPlaylist(User owner, String title) {
        Playlist playlist = new Playlist(owner, title, "설명입니다");
        return playlist;
    }

    public static User createTestUser(String email) {
        return new User(email, "password", "test-user", null, Role.USER);
    }

    public static Content createTestContent(String title) {
        return new Content(TMDB_ID_SEQ.getAndIncrement(), "movie", title, "설명", "abc.com", 0L, 0, 0);
    }

    private void updatePlaylistUpdatedAt(UUID id, Instant t) {

        EntityManager em = entityManager.getEntityManager();

        em.createQuery("UPDATE Playlist p SET p.updatedAt = :t WHERE p.id = :id")
                .setParameter("t", t)
                .setParameter("id", id)
                .executeUpdate();
    }

    private void updatePlaylistSubscriberCount(UUID id, long c) {

        EntityManager em = entityManager.getEntityManager();

        em.createQuery("UPDATE Playlist p SET p.subscriberCount = :c WHERE p.id = :id")
                .setParameter("c", c)
                .setParameter("id", id)
                .executeUpdate();
    }

}
