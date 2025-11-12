package com.codeit.playlist.domain.content.dto.request;

import java.util.List;
import java.util.UUID;

public record ContentCreateRequest(
        String type, // 콘텐츠 타입
        String title, // 콘텐츠 제목
        String description, // 콘텐츠 설명
        List<String> tags // 콘텐츠 태그 목록
) {
}
