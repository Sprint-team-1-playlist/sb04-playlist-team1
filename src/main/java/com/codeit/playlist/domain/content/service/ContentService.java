package com.codeit.playlist.domain.content.service;

import com.codeit.playlist.domain.content.dto.data.ContentDto;
import com.codeit.playlist.domain.content.dto.request.ContentCreateRequest;

public interface ContentService {
    ContentDto create(ContentCreateRequest request);
}
