package com.codeit.playlist.domain.watching.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class RedisWatchingSessionRepository {
    private final StringRedisTemplate redisTemplate;
    private static final String KEY_PREFIX = "content:%s:users";

    private String key(UUID contentId) {
        return KEY_PREFIX.formatted(contentId);
    }

    public boolean addWatcher(UUID contentId, UUID userId) {
        long timestamp = System.currentTimeMillis();
        Boolean result = redisTemplate.opsForZSet()
                .add(key(contentId), userId.toString(), timestamp);
        return Boolean.TRUE.equals(result);
    }

    public boolean removeWatcher(UUID contentId, UUID userId) {
        Long result = redisTemplate.opsForZSet()
                .remove(key(contentId), userId.toString());
        return result != null && result > 0;
    }

    public long countWatcher(UUID contentId) {
        Long result = redisTemplate.opsForSet().size(key(contentId));
        return result != null ? result : 0L;
    }
}
