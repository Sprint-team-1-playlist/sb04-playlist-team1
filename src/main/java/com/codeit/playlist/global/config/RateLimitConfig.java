package com.codeit.playlist.global.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimitConfig {
    @Bean
    public RedissonClient redissonClient(
            @Value("${spring.data.redis.host:localhost}") String host,
            @Value("${spring.data.redis.port:6379}") int port) {
        Config config = new Config();
        var serverConfig = config.useSingleServer()
                .setAddress(String.format("redis://%s:%d", host, port))
                .setConnectTimeout(5000)
                .setRetryAttempts(5)
                .setRetryInterval(2000);
        return Redisson.create(config);
    }
}
