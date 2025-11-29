package com.codeit.playlist.watching.repository;

import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.watching.dto.data.RawWatchingSession;
import com.codeit.playlist.domain.watching.dto.data.RawWatchingSessionPage;
import com.codeit.playlist.domain.watching.repository.RedisWatchingSessionRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataRedisTest
@Import(RedisWatchingSessionRepository.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("test")
class RedisWatchingSessionRepositoryTest {

    @Autowired
    private RedisWatchingSessionRepository repository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private UUID contentId;
    private UUID userId;
    private UUID watchingId;

    @BeforeEach
    void setup() {
        contentId = UUID.randomUUID();
        userId = UUID.randomUUID();
        watchingId = UUID.randomUUID();

        // Redis flush
        redisTemplate.execute((RedisCallback<Object>) connection -> {
            connection.serverCommands().flushDb();
            return null;
        });
    }

    @Test
    @DisplayName("addWatchingSession 정상 동작")
    void testAddWatchingSession() {
        RawWatchingSession raw = repository.addWatchingSession(watchingId, contentId, userId);

        String storedWatchingId = redisTemplate.opsForValue()
                .get("user:" + userId + ":session");
        assertThat(storedWatchingId).isEqualTo(watchingId.toString());

        Long zsetSize = redisTemplate.opsForZSet()
                .size("content:" + contentId + ":sessions");
        assertThat(zsetSize).isEqualTo(1L);

        assertThat(raw.userId()).isEqualTo(userId);
        assertThat(raw.contentId()).isEqualTo(contentId);
        assertThat(raw.watchingId()).isEqualTo(watchingId);
        assertThat(raw.createdAtEpoch()).isGreaterThan(0);
    }

    @Test
    @DisplayName("removeWatchingSession 정상 제거 및 Raw 반환")
    void testRemoveWatchingSession() {
        repository.addWatchingSession(watchingId, contentId, userId);

        RawWatchingSession removed = repository.removeWatchingSession(userId);

        assertThat(removed.userId()).isEqualTo(userId);
        assertThat(removed.contentId()).isEqualTo(contentId);

        assertThat(redisTemplate.opsForValue().get("user:" + userId + ":session"))
                .isNull();
        assertThat(redisTemplate.opsForHash().entries("watching:" + watchingId))
                .isEmpty();
        assertThat(redisTemplate.opsForZSet().size("content:" + contentId + ":sessions"))
                .isZero();
    }

    @Test
    @DisplayName("getAllWatchingSessionsByContentId 정상 조회")
    void testGetAllWatchingSessionsByContentId() {
        repository.addWatchingSession(watchingId, contentId, userId);

        List<RawWatchingSession> result = repository.getAllWatchingSessionsByContentId(contentId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).userId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("countWatchingSessionByContentId 정상 반환")
    void testCountWatchingSession() {
        repository.addWatchingSession(UUID.randomUUID(), contentId, userId);

        long count = repository.countWatchingSessionByContentId(contentId);

        assertThat(count).isEqualTo(1L);
    }

    @Test
    @DisplayName("getWatchingSessions: 첫 페이지 ASC 정상 조회")
    void testGetWatchingSessionsFirstPageASC() throws InterruptedException {
        // given
        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();
        UUID w1 = UUID.randomUUID();
        UUID w2 = UUID.randomUUID();

        RawWatchingSession s1 = repository.addWatchingSession(w1, contentId, u1);
        Thread.sleep(5);
        RawWatchingSession s2 = repository.addWatchingSession(w2, contentId, u2);

        // when
        RawWatchingSessionPage page = repository.getWatchingSessions(
                contentId,
                null,
                10,
                SortDirection.ASCENDING);

        // then
        assertThat(page.raws()).hasSize(2);
        assertThat(page.raws().get(0).createdAtEpoch())
                .isLessThan(page.raws().get(1).createdAtEpoch());
        assertThat(page.hasNext()).isFalse();
    }

    @Test
    @DisplayName("getWatchingSessions: 첫 페이지 DESC 정상 조회")
    void testGetWatchingSessionsFirstPageDESC() throws InterruptedException {
        // given
        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();
        UUID w1 = UUID.randomUUID();
        UUID w2 = UUID.randomUUID();

        RawWatchingSession s1 = repository.addWatchingSession(w1, contentId, u1);
        Thread.sleep(5);
        RawWatchingSession s2 = repository.addWatchingSession(w2, contentId, u2);

        // when
        RawWatchingSessionPage page = repository.getWatchingSessions(
                contentId,
                null,
                10,
                SortDirection.DESCENDING);

        // then
        assertThat(page.raws()).hasSize(2);
        assertThat(page.raws().get(0).createdAtEpoch())
                .isGreaterThan(page.raws().get(1).createdAtEpoch());
        assertThat(page.hasNext()).isFalse();
    }

    @Test
    @DisplayName("getWatchingSessions - 다음 페이지 ASC 조회")
    void testGetWatchingSessionsNextPageASC() throws InterruptedException {
        // given
        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();
        UUID u3 = UUID.randomUUID();

        UUID w1 = UUID.randomUUID();
        UUID w2 = UUID.randomUUID();
        UUID w3 = UUID.randomUUID();

        RawWatchingSession s1 = repository.addWatchingSession(w1, contentId, u1);
        Thread.sleep(5);
        RawWatchingSession s2 = repository.addWatchingSession(w2, contentId, u2);
        Thread.sleep(5);
        RawWatchingSession s3 = repository.addWatchingSession(w3, contentId, u3);

        RawWatchingSessionPage firstPage = repository.getWatchingSessions(
                contentId,
                null,
                1,
                SortDirection.ASCENDING);

        assertThat(firstPage.raws()).hasSize(1);
        long cursor = firstPage.raws().get(0).createdAtEpoch();

        // when - 두 번째 페이지
        RawWatchingSessionPage nextPage = repository.getWatchingSessions(
                contentId,
                String.valueOf(cursor),
                2,
                SortDirection.ASCENDING);

        // then
        assertThat(nextPage.raws()).hasSize(2);
        assertThat(nextPage.hasNext()).isFalse();
    }
}