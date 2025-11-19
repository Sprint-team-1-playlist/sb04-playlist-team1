package com.codeit.playlist.domain.content.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class TheSportsConfig {
    @Bean
    public WebClient theSportsClient() {
        return WebClient.builder()
                .baseUrl("https://www.thesportsdb.com/api/v1/json")
                .build();
    }
}
