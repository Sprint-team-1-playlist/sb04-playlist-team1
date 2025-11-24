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
            @Value("${spring.data.redis.port:6379}") int port,
            @Value("${spring.data.redis.password:}") String password) {
        Config config = new Config();
        var serverConfig = config.useSingleServer()
                .setAddress(String.format("redis://%s:%d", host, port));
        if (!password.isEmpty()) {
            serverConfig.setPassword(password);
        }
        return Redisson.create(config);
    }

}
