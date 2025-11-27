package com.codeit.playlist.domain.watching.repository;

import com.codeit.playlist.domain.watching.dto.data.RawWatchingSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

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

    // 입장
    public RawWatchingSession addWatchingSession(UUID watchingId, UUID contentId, UUID userId) {
        // 기존 세션이 있으면 먼저 제거
        String existingWatchingId = redisTemplate.opsForValue()
                .get(userKey(userId));
        if (existingWatchingId != null) {
            removeWatchingSession(userId);
        }

        long now = System.currentTimeMillis();
        redisTemplate.opsForValue()
                .set(userKey(userId), watchingId.toString());

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

    // 콘텐츠별 사용자 목록 조회
    public List<RawWatchingSession> getWatchingSessionsByContentId(UUID contentId) {
        Set<String> watchingIds = redisTemplate.opsForZSet()
                .range(contentKey(contentId), 0, -1);
        if (watchingIds == null) {
            return List.of();
        }

        List<RawWatchingSession> result = new ArrayList<>();
        for (String id : watchingIds) {
            UUID watchingId = UUID.fromString(id);

            Map<Object, Object> map = redisTemplate.opsForHash()
                    .entries(watchingKey(watchingId));
            if (map.isEmpty()) continue;

            UUID userId = UUID.fromString(map.get("userId").toString());
            long createdAtEpoch = Long.parseLong(map.get("createdAt").toString());

            result.add(new RawWatchingSession(watchingId, contentId, userId, createdAtEpoch));
        }

        return result;
    }

    // 콘텐츠별 사용자 수 조회
    public long countWatchingSessionByContentId(UUID contentId) {
        Long count = redisTemplate.opsForZSet()
                .size(contentKey(contentId));
        return count != null ? count : 0L;
    }
}
