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
            @Value("${spring.data.redis.port}") int port) {
        Config config = new Config();
        var serverConfig = config.useSingleServer()
                .setAddress(String.format("rediss://%s:%d", host, port)) // TLS
                .setSslEnableEndpointIdentification(false) // TLS 호스트 인증 비활성화
                .setConnectTimeout(10000) // 10초
                .setTimeout(10000) // Command timeout 10초
                .setRetryAttempts(10)
                .setRetryInterval(2000)
                .setConnectionPoolSize(20)     // Redisson connection pool
                .setConnectionMinimumIdleSize(5);

        return Redisson.create(config);
    }
}
