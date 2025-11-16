package com.codeit.playlist.watching.repository;

import com.codeit.playlist.domain.watching.repository.RedisWatchingSessionRepository;
import com.codeit.playlist.watching.fixture.WatchingSessionFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisWatchingSessionRepositoryTest {
    @InjectMocks
    private RedisWatchingSessionRepository redisWatchingSessionRepository;

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private SetOperations<String, String> setOps;

    private final UUID contentId = WatchingSessionFixtures.FIXED_ID;
    private final UUID userId = WatchingSessionFixtures.FIXED_ID;
    private final String key = "content:%s:user".formatted(contentId);

    @Test
    @DisplayName("addWatcher 호출 시 Redis set add 성공하면 true 반환")
    void addWatcherShouldReturnTrueWhenRedisAddSucceeds() {
        // given
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        when(setOps.add(key, userId.toString())).thenReturn(1L);

        // when
        boolean result = redisWatchingSessionRepository.addWatcher(contentId, userId);

        // then
        assertThat(result).isTrue();
        verify(setOps, times(1)).add(key, userId.toString());
    }

    @Test
    @DisplayName("addWatcher 호출 시 Redis set add가 실패하거나 null 이면 false 반환")
    void addWatcherShouldReturnFalseWhenRedisAddFails() {
        // given
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        when(setOps.add(key, userId.toString())).thenReturn(0L);

        // when
        boolean result = redisWatchingSessionRepository.addWatcher(contentId, userId);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("removeWatcher 호출 시 Redis set remove 성공하면 true 반환")
    void removeWatcherShouldReturnTrueWhenRedisRemoveSucceeds() {
        // given
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        when(setOps.remove(key, userId.toString())).thenReturn(1L);

        // when
        boolean result = redisWatchingSessionRepository.removeWatcher(contentId, userId);

        // then
        assertThat(result).isTrue();
        verify(redisTemplate, times(1)).opsForSet();
    }

    @Test
    @DisplayName("removeWatcher 호출 시 Redis set remove 실패하면 false 반환")
    void removeWatcherShouldReturnFalseWhenRedisRemoveFails() {
        // given
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        when(setOps.remove(key, userId.toString())).thenReturn(0L);

        // when
        boolean result = redisWatchingSessionRepository.removeWatcher(contentId, userId);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("countWatcher 호출 시 Redis SET size 값 반환")
    void countWatcherShouldReturnSizeFromRedis() {
        // given
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        when(setOps.size(key)).thenReturn(5L);

        // when
        long count = redisWatchingSessionRepository.countWatcher(contentId);

        // then
        assertThat(count).isEqualTo(5L);
        verify(setOps, times(1)).size(key);
    }

    @Test
    @DisplayName("countWatcher 호출 시 null 이면 0 반환")
    void countWatcherShouldReturnZeroWhenRedisReturnsNull() {
        // given
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        when(setOps.size(key)).thenReturn(null);

        // when
        long count = redisWatchingSessionRepository.countWatcher(contentId);

        // then
        assertThat(count).isZero();
    }
}