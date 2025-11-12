package com.codeit.playlist.domain.content.dto.data;

import java.util.List;
import java.util.UUID;

public record ContentSummary(
        UUID id,    // 콘텐츠 ID
        String type,    // 콘텐츠 타입
        String title,   // 콘텐츠 제목
        String description,    // 콘텐츠 설명
        String thumbnailUrl,    // 썸네일 이미지 URL
        List<String> tags,  // 콘텐츠 태그 목록
        Double averageRating,   // 평균 평점
        Integer reviewCount     // 리뷰 개수
) {
}
