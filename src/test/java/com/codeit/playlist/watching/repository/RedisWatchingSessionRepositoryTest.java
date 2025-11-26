package com.codeit.playlist.watching.repository;

import com.codeit.playlist.domain.watching.dto.data.RawWatchingSession;
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
    @DisplayName("getWatchingSessionsByContentId 정상 조회")
    void testGetWatchingSessionsByContentId() {
        repository.addWatchingSession(watchingId, contentId, userId);

        List<RawWatchingSession> result = repository.getWatchingSessionsByContentId(contentId);

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
}