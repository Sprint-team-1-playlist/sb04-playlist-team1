package com.codeit.playlist.domain.auth.passwordRateLimit;

import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RateLimitService {

  private final RedissonClient redissonClient;

  private static final int MAX_REQ = 5;       // 5번 허용
  private static final int WINDOW_SEC = 60;   // 1분 동안

  public boolean isAllowed(String key) {
    RAtomicLong counter = redissonClient.getAtomicLong(key);

    long current = counter.incrementAndGet();

    if (current == 1) {
      // 첫 요청이므로 TTL 설정
      counter.expire(WINDOW_SEC, TimeUnit.SECONDS);
    }

    return current <= MAX_REQ;
    }
  }
