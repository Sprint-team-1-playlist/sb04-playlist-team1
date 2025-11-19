package com.codeit.playlist.domain.auth.passwordRateLimit;

import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RateLimitService {

  private final RedissonClient redissonClient;

  private static final int MAX_REQ = 5;       // 5번 허용
  private static final int WINDOW_SEC = 60;   // 1분 동안

    public boolean isAllowed(String key) {
    RBucket<Integer> bucket = redissonClient.getBucket(key);
    Integer count = bucket.get();

    if (count == null) {
      bucket.set(1, WINDOW_SEC, TimeUnit.SECONDS);
      return true;
    }

    if (count < MAX_REQ) {
      bucket.set(count + 1);
      return true;
    }

    return false; // 초과 → 차단
  }

}
