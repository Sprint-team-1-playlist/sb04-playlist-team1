package com.codeit.playlist.domain.content.aws.service.basic;

import com.codeit.playlist.domain.content.aws.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3FileStorageService implements FileStorageService {
    private final S3Client s3Client;

    @Value("${app.file-upload.bucket}")
    private String bucket;

    @Value("${app.file-upload.directory}")
    private String directory;

    @Value("${app.file-upload.base-url}")
    private String baseUrl;

    @Override
    public String put(MultipartFile file) {
        log.debug("S3 썸네일 업로드 시작, OriginalFilename : {}", file.getOriginalFilename());
        if(file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 없어요.");
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String extractFilename = ""; // 초기화

            if(originalFilename != null && originalFilename.contains(".")) {
                extractFilename = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String imageUri = directory + UUID.randomUUID() + extractFilename; // S3 URI, 이미지 주소

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(imageUri)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize())); // 예외필요

            String url = baseUrl + "/" + imageUri; // 버킷명/imageUri

            log.info("S3 썸네일 업로드 완료, url : {}", url);
            return url;

        } catch(IOException e) {
            log.error("S3 썸네일 업로드 실패", e);
            throw new RuntimeException("S3 썸네일 업로드 실패",e);
        }
    }
}
