package com.codeit.playlist.domain.content.service;

import com.codeit.playlist.domain.content.dto.data.ContentDto;
import com.codeit.playlist.domain.content.dto.request.ContentCreateRequest;
import com.codeit.playlist.domain.content.dto.request.ContentUpdateRequest;

import java.util.UUID;

public interface ContentService {
    ContentDto create(ContentCreateRequest request, String thumbnail);
    ContentDto update(UUID contentId, ContentUpdateRequest request, String thumbnail);
    void delete(UUID contentId);
}
