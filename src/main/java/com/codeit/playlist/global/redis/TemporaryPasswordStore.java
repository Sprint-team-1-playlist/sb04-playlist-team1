package com.codeit.playlist.global.redis;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TemporaryPasswordStore {

  private final StringRedisTemplate redisTemplate;
  private static final String PREFIX = "temp-password:";

  public void save(UUID userId, String password, long ttlSeconds){
    String key = PREFIX + userId;
    redisTemplate.opsForValue().set(key, password, ttlSeconds, TimeUnit.SECONDS);
  }

  public boolean isValid(UUID userId, String providedPassword){
    String key = PREFIX + userId;
    String stored = redisTemplate.opsForValue().get(key);
    return stored != null && stored.equals(providedPassword);
  }

  public void delete(UUID userId){
    redisTemplate.delete(PREFIX + userId);
  }

}
