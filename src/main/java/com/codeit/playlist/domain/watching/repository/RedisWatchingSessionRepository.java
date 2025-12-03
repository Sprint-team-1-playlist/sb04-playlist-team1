package com.codeit.playlist.domain.watching.repository;

import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.watching.dto.data.RawContentChat;
import com.codeit.playlist.domain.watching.dto.data.RawWatchingSession;
import com.codeit.playlist.domain.watching.dto.data.RawWatchingSessionPage;
import com.codeit.playlist.domain.watching.exception.WatchingNotFoundException;
import com.codeit.playlist.domain.watching.exception.WatchingSessionMismatch;
import com.codeit.playlist.global.error.InvalidCursorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.*;

/*
 * Redis 설계
 * 1. String user:{userId}:session (사용자의 현재 세션 정보)
 * value = watchingId
 *
 * 2. ZSET: score로 정렬하기 위함(커서 페이지네이션에 사용)
 * content:{contentId}:sessions
 * members = watchingId1, watchingId2, watchingId3, ...
 * score = currentTimeMillis
 *
 * 3. HASH: 시청세션의 상세정보를 조회하기 위함
 * watching:{watchingId}
 * contentId
 * userId
 * createdAt
 *
 * 4. List: 콘텐츠별 채팅 내역 보관
 * content:{contentId}:chat:list
 * value: JSON(sender, content)
 * */

@Repository
@RequiredArgsConstructor
@Slf4j
public class RedisWatchingSessionRepository {
    private final StringRedisTemplate redisTemplate;

    private String userKey(UUID userId) {
        return "user:" + userId + ":session";
    }

    private String contentKey(UUID contentId) {
        return "content:" + contentId + ":sessions";
    }

    private String watchingKey(UUID watchingId) {
        return "watching:" + watchingId;
    }

    private String chatKey(UUID contentId) {
        return "content:" + contentId + ":chat:list";
    }

    // 입장
    public RawWatchingSession addWatchingSession(UUID watchingId, UUID contentId, UUID userId) {
        // 기존 세션이 있으면 먼저 제거
        String existingWatchingId = redisTemplate.opsForValue()
                .get(userKey(userId));
        if (existingWatchingId != null) {
            removeWatchingSession(userId);
        }

        redisTemplate.opsForValue()
                .set(userKey(userId), watchingId.toString());

        long now = System.currentTimeMillis();
        redisTemplate.opsForZSet()
                .add(contentKey(contentId), watchingId.toString(), now);

        Map<String, String> map = new HashMap<>();
        map.put("contentId", contentId.toString());
        map.put("userId", userId.toString());
        map.put("createdAt", String.valueOf(now));
        redisTemplate.opsForHash()
                .putAll(watchingKey(watchingId), map);

        return new RawWatchingSession(watchingId, contentId, userId, now);
    }

    // 퇴장
    public RawWatchingSession removeWatchingSession(UUID userId) {
        String watchingIdStr = redisTemplate.opsForValue()
                .get(userKey(userId));
        if (watchingIdStr == null) {
            return null;
        }
        UUID watchingId = UUID.fromString(watchingIdStr);

        Map<Object, Object> map = redisTemplate.opsForHash()
                .entries(watchingKey(watchingId));
        if (map.isEmpty()) {
            redisTemplate.delete(userKey(userId));
            return null;
        }

        UUID contentId = UUID.fromString(map.get("contentId").toString());
        UUID uid = UUID.fromString(map.get("userId").toString());
        long createdAtEpoch = Long.parseLong(map.get("createdAt").toString());

        redisTemplate.opsForZSet()
                .remove(contentKey(contentId), watchingIdStr);
        redisTemplate.delete(watchingKey(watchingId));
        redisTemplate.delete(userKey(userId));

        return new RawWatchingSession(watchingId, contentId, uid, createdAtEpoch);
    }

    // 콘텐츠별 사용자 목록 전체 조회
    public List<RawWatchingSession> getAllWatchingSessionsByContentId(UUID contentId) {
        Set<String> watchingIds = redisTemplate.opsForZSet()
                .range(contentKey(contentId), 0, -1);
        if (watchingIds == null) {
            return List.of();
        }

        List<RawWatchingSession> raws = new ArrayList<>();
        getRawSessions(contentId, watchingIds, raws);

        return raws;
    }

    // 콘텐츠별 사용자 목록 조회(커서페이지네이션)
    public RawWatchingSessionPage getWatchingSessionsByContentId(UUID contentId,
                                                                 String cursor,
                                                                 int limit,
                                                                 SortDirection sortDirection) {
        Set<String> watchingIds;
        if (cursor == null) {
            watchingIds = getFirstPage(
                    contentId,
                    limit,
                    sortDirection);
        } else {
            watchingIds = getNextPage(
                    contentId,
                    cursor,
                    limit,
                    sortDirection);
        }

        List<RawWatchingSession> raws = getPageDetails(contentId, watchingIds);

        boolean hasNext = (raws.size() > limit);
        if (hasNext) {
            raws = raws.subList(0, limit);
        }

        return new RawWatchingSessionPage(
                raws,
                hasNext
        );
    }

    // 콘텐츠별 사용자 수 조회
    public long countWatchingSessionByContentId(UUID contentId) {
        Long count = redisTemplate.opsForZSet()
                .size(contentKey(contentId));
        return count != null ? count : 0L;
    }

    // 특정 사용자의 시청 세션 조회
    public RawWatchingSession getWatchingSessionByUser(UUID userId) {
        String watchingIdStr = redisTemplate.opsForValue()
                .get(userKey(userId));
        if (watchingIdStr == null) {
            log.debug("[실시간 같이 보기] userId({})에 대한 watchingId가 없음", userId);
            return null;
        }

        UUID watchingId = UUID.fromString(watchingIdStr);
        Map<Object, Object> map = redisTemplate.opsForHash()
                .entries(watchingKey(watchingId));
        if (map.isEmpty()) {
            log.error("[실시간 같이 보기] 세선 정보 없음: watchingId={}", watchingId);
            throw WatchingNotFoundException.withId(watchingId);
        }
        if (!convertObjectToUuid(map, "userId").equals(userId)) {
            log.error("[실시간 같이 보기] 사용자 정보가 일치하지 않음: watchingId={}, userId={}", watchingId, userId);
            throw WatchingSessionMismatch.withWatchingIdAndUserId(watchingId, userId);
        }

        return new RawWatchingSession(
                watchingId,
                convertObjectToUuid(map, "contentId"),
                convertObjectToUuid(map, "userId"),
                convertObjectToLong(map, "createdAt")
        );
    }

    // 채팅
    public RawContentChat addChat(UUID contentId, UUID senderId, String content) {
        String chatData = senderId.toString() + ":" + content;
        Long raw = redisTemplate.opsForList()
                .rightPush(chatKey(contentId), chatData);
        if (raw == null) {
            return null;
        }

        redisTemplate.expire(chatKey(contentId), Duration.ofMinutes(30));

        return new RawContentChat(
                senderId,
                content
        );
    }

    // 헬퍼 메서드
    private Set<String> getFirstPage(UUID contentId,
                                     int limit,
                                     SortDirection sortDirection) {
        if (sortDirection.equals(SortDirection.ASCENDING)) {
            return redisTemplate.opsForZSet()
                    .range(contentKey(contentId), 0, limit + 1);
        }

        return redisTemplate.opsForZSet()
                .reverseRange(contentKey(contentId), 0, limit + 1);
    }

    private Set<String> getNextPage(UUID contentId,
                                    String cursor,
                                    int limit,
                                    SortDirection sortDirection) {
        long cursorLong;
        try {
            cursorLong = Long.parseLong(cursor);
        } catch (NumberFormatException e) {
            log.error("[실시간 같이 보기] 유효하지 않은 커서: cursor={}", cursor);
            throw InvalidCursorException.withCursor(cursor);
        }

        if (sortDirection.equals(SortDirection.ASCENDING)) {
            return redisTemplate.opsForZSet()
                    .rangeByScore(contentKey(contentId), cursorLong + 1, Long.MAX_VALUE, 0, limit + 1);
        }

        return redisTemplate.opsForZSet()
                .reverseRangeByScore(contentKey(contentId), 0, cursorLong - 1, 0, limit + 1);
    }

    private List<RawWatchingSession> getPageDetails(UUID contentId, Set<String> watchingIds) {
        List<RawWatchingSession> raws = new ArrayList<>();
        if (watchingIds == null) {
            return raws;
        }

        getRawSessions(contentId, watchingIds, raws);

        return raws;
    }

    private void getRawSessions(UUID contentId, Set<String> watchingIds, List<RawWatchingSession> raws) {
        for (String id : watchingIds) {
            UUID watchingId = UUID.fromString(id);

            Map<Object, Object> map = redisTemplate.opsForHash()
                    .entries(watchingKey(watchingId));
            if (map.isEmpty()) continue;

            UUID userId = UUID.fromString(map.get("userId").toString());
            long createdAtEpoch = Long.parseLong(map.get("createdAt").toString());

            raws.add(new RawWatchingSession(watchingId, contentId, userId, createdAtEpoch));
        }
    }

    private UUID convertObjectToUuid(Map<Object, Object> map, Object key) {
        return UUID.fromString(map.get(key).toString());
    }

    private long convertObjectToLong(Map<Object, Object> map, Object key) {
        return Long.parseLong(map.get(key).toString());
    }
}
