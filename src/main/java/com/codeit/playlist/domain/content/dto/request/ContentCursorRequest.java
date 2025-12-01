package com.codeit.playlist.domain.content.dto.request;

import com.codeit.playlist.domain.base.SortDirection;

import java.util.List;

public record ContentCursorRequest(
        String typeEqual, // 콘텐츠 타입 Available values : movie, tvSeries, sport
        String keywordLike, // 검색 키워드
        List<String> tagsIn, // 태그 목록, tag가 리스트니까 컨텐츠 하나에 대한 request임
        String cursor, // 커서
        String idAfter, // 보조 커서
        int limit, // 한번에 가져올 개수
        SortDirection sortDirection, // 정렬 방향, Available values : ASCENDING, DECENDING
        String sortBy // 정렬 기준, Available values : createdAt, watcherCount, rate
) {
}
