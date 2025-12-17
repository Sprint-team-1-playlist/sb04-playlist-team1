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
            @Value("${spring.data.redis.host}") String host,
            @Value("${spring.data.redis.port}") int port,
            @Value("${spring.data.redis.ssl.enabled}") boolean ssl) {

        Config config = new Config();
        var serverConfig = config.useSingleServer()
                .setAddress((ssl ? "rediss://" : "redis://") + host + ":" + port) // TLS
                .setConnectTimeout(60000) // 60초
                .setTimeout(60000) // Command timeout 60초
                .setRetryAttempts(10)
                .setConnectionPoolSize(32)     // Redisson connection pool: 64(default)
                .setConnectionMinimumIdleSize(8)
                .setSubscriptionConnectionPoolSize(16)
                .setSubscriptionConnectionMinimumIdleSize(4);

        return Redisson.create(config);
    }
}
