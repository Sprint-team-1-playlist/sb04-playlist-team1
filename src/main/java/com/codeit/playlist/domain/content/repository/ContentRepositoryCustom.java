package com.codeit.playlist.domain.content.repository;

import com.codeit.playlist.domain.content.dto.request.ContentCursorRequest;
import com.codeit.playlist.domain.content.entity.Content;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContentRepositoryCustom {
    List<Content> searchContents(ContentCursorRequest request, boolean ascending, int limit, String sortBy);

    long countContents(ContentCursorRequest request);
}
