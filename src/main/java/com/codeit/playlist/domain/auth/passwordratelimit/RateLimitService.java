package com.codeit.playlist.domain.auth.passwordratelimit;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RateLimitService {

  private final RedissonClient redissonClient;

  private static final int MAX_REQ = 5;       // 5번 허용
  private static final int WINDOW_SEC = 60;   // 1분 동안

  public boolean isAllowed(String key) {
    RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
    if (!rateLimiter.isExists()) {
      rateLimiter.trySetRate(RateType.OVERALL, MAX_REQ, WINDOW_SEC, RateIntervalUnit.SECONDS);
    }
    return rateLimiter.tryAcquire();
  }
}
