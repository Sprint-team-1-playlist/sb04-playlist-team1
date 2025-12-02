package com.codeit.playlist;

import com.codeit.playlist.domain.content.api.handler.TheSportsDateHandler;
import com.codeit.playlist.domain.security.AdminInitializer;
import com.codeit.playlist.domain.security.jwt.JwtTokenProvider;
import com.codeit.playlist.global.config.TheSportsConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.services.s3.S3Client;

@SpringBootTest
@ActiveProfiles("test")
class PlaylistApplicationTests {

    @MockitoBean
    private S3Client s3Client;

    @MockitoBean
    private TheSportsConfig theSportsConfig;

    @MockitoBean
    private TheSportsDateHandler theSportsDateHandler;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @MockitoBean
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @MockitoBean
    private AdminInitializer adminInitializer;

    @Test
    void contextLoads() {
    }

}
