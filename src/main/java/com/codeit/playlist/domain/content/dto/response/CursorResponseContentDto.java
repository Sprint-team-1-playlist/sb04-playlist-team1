package com.codeit.playlist.domain.content.dto.response;

import com.codeit.playlist.domain.content.dto.data.ContentDto;

import java.util.List;

public record CursorResponseContentDto(
        List<ContentDto> data, // 데이터 목록, 콘텐츠
        String nextCursor, // 다음 커서
        String nextIdAfter, // 다음 요청의 보조 커서
        Boolean hasNext, // 다음 데이터가 있는지 여부
        Integer pageSize, // 총 데이터 개수
        String sortBy, // 정렬 기준
        String sortDirection // 정렬 방향
) {
}
