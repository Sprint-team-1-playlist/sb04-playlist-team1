package com.codeit.playlist.global.constant;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "cloud.aws.s3")
public class S3Properties {
    private String contentBucket;
    private String profileBucket;
    private String logsBucket;
    private String region;
}
