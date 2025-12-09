package com.codeit.playlist.domain.file;

import com.codeit.playlist.domain.file.exception.FailDeleteFromS3;
import com.codeit.playlist.domain.file.exception.FailUploadToS3Exception;
import com.codeit.playlist.global.constant.S3Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/*
 * 콘텐츠와 프로필 이미지는 GetObject 요청에 대해서만 Public 함
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class S3Uploader {
    private final S3Client s3Client;
    private final S3Properties s3Properties;

    public String upload(String bucket, String key, MultipartFile file) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType()) // MultipartFile -> byte[]로 변환해 AWS SDK가 읽을 수 있게 변환
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));

            return generateFileUrl(bucket, key);
        } catch (Exception e) {
            log.error("[S3] S3에 파일 업로드 실패: key={}, bucket={}, errorMessage={}", key, bucket, e.getMessage(), e);
            throw FailUploadToS3Exception.withBucket(bucket);
        }
    }

    public void delete(String bucket, String key) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            s3Client.deleteObject(request);
        } catch (Exception e) {
            log.error("[S3] S3에서 파일 삭제 실패: key={}, bucket={}, errorMessage={}", key, bucket, e.getMessage(), e);
            throw FailDeleteFromS3.withBucket(bucket);
        }
    }

    public void uploadLogs(String bucket, String key, File file) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType("text/plain")
                    .build();

            s3Client.putObject(request, RequestBody.fromFile(file));
        } catch (Exception e) {
            log.error("[S3] S3에 로그 파일 업로드 실패: key={}, bucket={}, errorMessage={}", key, bucket, e.getMessage(), e);
            throw FailUploadToS3Exception.withBucket(bucket);
        }
    }

    private String generateFileUrl(String bucket, String key) {
        String encodedKey = URLEncoder.encode(key, StandardCharsets.UTF_8).replace("+", "%20");
        return "https://" + bucket + ".s3." + s3Properties.getRegion() + ".amazonaws.com/" + encodedKey;
    }
}
