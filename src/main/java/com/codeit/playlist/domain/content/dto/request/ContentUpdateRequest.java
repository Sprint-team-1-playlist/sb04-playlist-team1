package com.codeit.playlist.domain.content.dto.request;

import com.codeit.playlist.domain.content.entity.tags;

import java.util.List;

public record ContentUpdateRequest(
        String title, // 콘텐츠 목록
        String description, // 콘텐츠 설명
        List<String> tags // 콘텐츠 태그 목록
) {
}
