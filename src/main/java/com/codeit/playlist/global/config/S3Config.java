package com.codeit.playlist.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@EnableConfigurationProperties(S3Properties.class)
public class S3Config { // S3와 통신하는 클라이언트 객체
    @Value("${cloud.aws.s3.region}")
    private String region;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .build();
    }

    @Bean
    public S3Presigner s3Presigner(S3Properties s3Properties) {
        return S3Presigner.builder()
                .region(Region.of(s3Properties.getRegion()))
                .build();
    }
}
