package com.codeit.playlist.watching.repository;

import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.watching.dto.data.RawContentChat;
import com.codeit.playlist.domain.watching.dto.data.RawWatchingSession;
import com.codeit.playlist.domain.watching.dto.data.RawWatchingSessionPage;
import com.codeit.playlist.domain.watching.exception.WatchingNotFoundException;
import com.codeit.playlist.domain.watching.exception.WatchingSessionMismatch;
import com.codeit.playlist.domain.watching.repository.RedisWatchingSessionRepository;
import com.codeit.playlist.global.error.InvalidCursorException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataRedisTest
@Import(RedisWatchingSessionRepository.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("test")
class RedisWatchingSessionRepositoryTest {

    @Autowired
    private RedisWatchingSessionRepository repository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;
    @TestConfiguration
    static class TestConfig {
        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

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
                .get("user:" + userId + ":watching");
        assertThat(storedWatchingId).isEqualTo(watchingId.toString());

        Long zsetSize = redisTemplate.opsForZSet()
                .size("content:" + contentId + ":watching");
        assertThat(zsetSize).isEqualTo(1L);

        assertThat(raw.userId()).isEqualTo(userId);
        assertThat(raw.contentId()).isEqualTo(contentId);
        assertThat(raw.watchingId()).isEqualTo(watchingId);
        assertThat(raw.createdAtEpoch()).isGreaterThan(0);
    }

    @Test
    @DisplayName("removeWatchingSession 정상 제거 및 Raw 반환 (채팅도 함께 제거되는지 확인)")
    void testRemoveWatchingSession() throws Exception {
        repository.addWatchingSession(watchingId, contentId, userId);

        RawContentChat rawChat = repository.addChat(contentId, userId, "hello-remove-test");
        Set<String> chatZset = redisTemplate.opsForZSet().range("content:" + contentId + ":chat", 0, -1);
        List<String> chatList = redisTemplate.opsForList().range("content:" + contentId + ":chat:" + userId, 0, -1);

        assertThat(chatZset).isNotEmpty();
        assertThat(chatList).isNotEmpty();

        // when
        RawWatchingSession removed = repository.removeWatchingSession(userId);

        // then
        assertThat(removed).isNotNull();
        assertThat(removed.userId()).isEqualTo(userId);
        assertThat(removed.contentId()).isEqualTo(contentId);

        assertThat(redisTemplate.opsForValue().get("user:" + userId + ":watching"))
                .isNull();
        assertThat(redisTemplate.opsForHash().entries("watching:" + watchingId))
                .isEmpty();
        assertThat(redisTemplate.opsForZSet().size("content:" + contentId + ":watching"))
                .isZero();

        List<String> chatListAfter = redisTemplate.opsForList().range("content:" + contentId + ":chat:" + userId, 0, -1);
        assertThat(chatListAfter).isNullOrEmpty();

        Set<String> chatZsetAfter = redisTemplate.opsForZSet().range("content:" + contentId + ":chat", 0, -1);
        if (chatZsetAfter != null) {
            for (String m : chatZsetAfter) {
                RawContentChat rc = objectMapper.readValue(m, RawContentChat.class);
                assertThat(rc.userId()).isNotEqualTo(userId);
            }
        }
    }

    @Test
    @DisplayName("removeWatchingSession, 세션 없으면 null 반환")
    void testRemoveWatchingSessionNull() {
        RawWatchingSession result = repository.removeWatchingSession(userId);
        assertThat(result).isNull();
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

        repository.addWatchingSession(w1, contentId, u1);
        Thread.sleep(5);
        repository.addWatchingSession(w2, contentId, u2);

        // when
        RawWatchingSessionPage page = repository.getWatchingSessionsByContentId(
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

        repository.addWatchingSession(w1, contentId, u1);
        Thread.sleep(5);
        repository.addWatchingSession(w2, contentId, u2);

        // when
        RawWatchingSessionPage page = repository.getWatchingSessionsByContentId(
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

        repository.addWatchingSession(w1, contentId, u1);
        Thread.sleep(5);
        repository.addWatchingSession(w2, contentId, u2);
        Thread.sleep(5);
        repository.addWatchingSession(w3, contentId, u3);

        RawWatchingSessionPage firstPage = repository.getWatchingSessionsByContentId(
                contentId,
                null,
                1,
                SortDirection.ASCENDING);

        assertThat(firstPage.raws()).hasSize(1);
        long cursor = firstPage.raws().get(0).createdAtEpoch();

        // when - 두 번째 페이지
        RawWatchingSessionPage nextPage = repository.getWatchingSessionsByContentId(
                contentId,
                String.valueOf(cursor),
                2,
                SortDirection.ASCENDING);

        // then
        assertThat(nextPage.raws()).hasSize(2);
        assertThat(nextPage.hasNext()).isFalse();
    }

    @Test
    @DisplayName("getWatchingSessionsByContentId: 유효하지 않은 커서 예외 발생")
    void testGetWatchingSessionsInvalidCursor() {
        assertThatThrownBy(() -> repository.getWatchingSessionsByContentId(
                contentId,
                "invalidCursor",
                5,
                SortDirection.ASCENDING
        )).isInstanceOf(InvalidCursorException.class);
    }

    @Test
    @DisplayName("사용자의 시청 세션 조회, 세션이 없으면 null 반환")
    void getWatchingSessionByUserReturnNullIfNotExists() {
        RawWatchingSession result = repository.getWatchingSessionByUser(userId);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("사용자의 시청 세션 조회, HASH 정보 없으면 WatchingNotFoundException 발생")
    void getWatchingSessionByUser_hashNotFound() {
        // given: user -> watchingId 는 존재하지만 hash 는 삭제된 상태
        redisTemplate.opsForValue().set("user:" + userId + ":watching", watchingId.toString());

        assertThatThrownBy(() -> repository.getWatchingSessionByUser(userId))
                .isInstanceOf(WatchingNotFoundException.class);
    }

    @Test
    @DisplayName("사용자의 시청 세션 조회, userId가 동일하지 않으면 WatchingSessionMismatch 발생")
    void getWatchingSessionByUserMismatch() {
        repository.addWatchingSession(watchingId, contentId, userId);

        redisTemplate.opsForHash().put("watching:" + watchingId, "userId", UUID.randomUUID().toString());

        assertThatThrownBy(() -> repository.getWatchingSessionByUser(userId))
                .isInstanceOf(WatchingSessionMismatch.class);
    }

    @Test
    @DisplayName("사용자의 시청 세션 조회, 정상 조회 성공")
    void getWatchingSessionByUserSuccess() {
        repository.addWatchingSession(watchingId, contentId, userId);

        RawWatchingSession raw = repository.getWatchingSessionByUser(userId);

        assertThat(raw.watchingId()).isEqualTo(watchingId);
        assertThat(raw.userId()).isEqualTo(userId);
        assertThat(raw.contentId()).isEqualTo(contentId);
    }

    @Test
    @DisplayName("addChat 정상 동작 (ZSet + user list 에 JSON 저장되는지 확인)")
    void testAddChat() throws Exception {
        // given
        String content = "content";

        // when
        RawContentChat rawChat = repository.addChat(contentId, userId, content);

        // then
        assertThat(rawChat.userId()).isEqualTo(userId);
        assertThat(rawChat.content()).isEqualTo(content);

        // zset members (JSON)
        Set<String> chatZset = redisTemplate.opsForZSet().range("content:" + contentId + ":chat", 0, -1);
        assertThat(chatZset).isNotNull().hasSize(1);

        // list for user
        List<String> chatList = redisTemplate.opsForList().range("content:" + contentId + ":chat:" + userId, 0, -1);
        assertThat(chatList).isNotNull().hasSize(1);

        // deserialize JSON to RawContentChat and verify fields
        String jsonInZset = chatZset.iterator().next();
        RawContentChat deserialized = objectMapper.readValue(jsonInZset, RawContentChat.class);
        assertThat(deserialized.userId()).isEqualTo(userId);
        assertThat(deserialized.content()).isEqualTo(content);

        String jsonInList = chatList.get(0);
        RawContentChat deserializedList = objectMapper.readValue(jsonInList, RawContentChat.class);
        assertThat(deserializedList.userId()).isEqualTo(userId);
        assertThat(deserializedList.content()).isEqualTo(content);
    }
}