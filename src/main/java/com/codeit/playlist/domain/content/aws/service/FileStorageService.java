package com.codeit.playlist.domain.content.aws.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String put(MultipartFile file);
}
