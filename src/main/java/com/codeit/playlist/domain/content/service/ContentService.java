package com.codeit.playlist.domain.content.service;

import com.codeit.playlist.domain.content.dto.data.ContentDto;
import com.codeit.playlist.domain.content.dto.request.ContentCreateRequest;
import com.codeit.playlist.domain.content.dto.request.ContentCursorRequest;
import com.codeit.playlist.domain.content.dto.request.ContentUpdateRequest;
import com.codeit.playlist.domain.content.dto.response.CursorResponseContentDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface ContentService {
    ContentDto create(ContentCreateRequest request, MultipartFile thumbnail);
    ContentDto update(UUID contentId, ContentUpdateRequest request, String thumbnail);
    void delete(UUID contentId);
    CursorResponseContentDto get(ContentCursorRequest request);
    ContentDto search(UUID contentId);
    String saveImageToS3(MultipartFile file);
}
