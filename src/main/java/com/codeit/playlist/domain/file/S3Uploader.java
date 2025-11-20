package com.codeit.playlist.domain.file;

import com.codeit.playlist.domain.file.exception.FailDeleteFromS3;
import com.codeit.playlist.domain.file.exception.FailGeneratePresignedUrl;
import com.codeit.playlist.domain.file.exception.FailUploadToS3Exception;
import com.codeit.playlist.global.config.S3Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class S3Uploader {
    private final S3Client s3Client;
    private final S3Properties s3Properties;
    private final S3Presigner s3Presigner;

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
            log.error("[S3] 파일을 S3에 업로드하지 못함: key={}, bucket={}, errorMessage={}", key, bucket, e.getMessage(), e);
            throw FailUploadToS3Exception.withBucket(bucket);
        }
    }

    public String uploadLogs(String bucket, String key, MultipartFile file) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType()) // MultipartFile -> byte[]로 변환해 AWS SDK가 읽을 수 있게 변환
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));

            return getPresignedUrl(bucket, key);
        } catch (Exception e) {
            log.error("[S3] 로그 파일을 S3에 업로드하지 못함: key={}, bucket={}, errorMessage={}", key, bucket, e.getMessage(), e);
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
            log.error("[S3] 파일을 S3에서 삭제하지 못함: key={}, bucket={}, errorMessage={}", key, bucket, e.getMessage(), e);
            throw FailDeleteFromS3.withBucket(bucket);
        }
    }

    public String getPresignedUrl(String bucket, String key) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            PresignedGetObjectRequest getPresigned = s3Presigner.presignGetObject(
                    GetObjectPresignRequest.builder()
                            .signatureDuration(Duration.ofMinutes(10))
                            .getObjectRequest(request)
                            .build()
            );

            return getPresigned.url().toString();
        } catch (Exception e) {
            log.error("[S3] presigned URL 생성 실패: key={}, bucket={}, errorMessage={}", key, bucket, e.getMessage(), e);
            throw FailGeneratePresignedUrl.withBucket(bucket);
        }
    }

    private String generateFileUrl(String bucket, String key) {
        return "https://" + bucket + ".s3." + s3Properties.getRegion() + ".amazonaws.com/" + key;
    }
}
