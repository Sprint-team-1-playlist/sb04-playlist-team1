package com.codeit.playlist.global.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimitConfig {

  @Value("${spring.data.redis.host:localhost}")
  private String redisHost;

  @Value("${spring.data.redis.port:6379}")
  private int redisPort;

  @Value("${spring.data.redis.password:}")
  private String redisPassword;

  @Bean
  public RedissonClient redissonClient() {
    Config config = new Config();
    var serverConfig = config.useSingleServer()
        .setAddress(String.format("redis://%s:%d", redisHost, redisPort));
    if (!redisPassword.isEmpty()) {
      serverConfig.setPassword(redisPassword);
    }
    return Redisson.create(config);
  }

}
