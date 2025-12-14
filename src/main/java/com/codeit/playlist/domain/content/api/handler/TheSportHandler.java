package com.codeit.playlist.domain.content.api.handler;

import com.codeit.playlist.domain.content.api.mapper.TheSportMapper;
import com.codeit.playlist.domain.content.api.response.TheSportResponse;
import com.codeit.playlist.domain.content.api.service.TheSportApiService;
import com.codeit.playlist.domain.content.entity.Content;
import com.codeit.playlist.domain.content.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TheSportHandler {
    private final ContentRepository contentRepository;
    private final TheSportApiService theSportApiService;
    private final TheSportMapper theSportsMapper;

    @Transactional
    public void save(int year, int month) {
        List<TheSportResponse> listResponse = theSportApiService.getApiSport(year, month)
                .collectList()
                .doOnNext(resultList -> log.info("[콘텐츠 데이터 관리] year : {}, month : {}", year, month))
                .block();

        if(listResponse == null || listResponse.isEmpty()) {
            log.debug("[콘텐츠 데이터 관리] SportList가 비어있습니다, year : {}, month : {}", year, month);
            return;
        }

        for(int i = 0; i< listResponse.size(); i++) {
            TheSportResponse theSportResponse = listResponse.get(i);
            if(theSportResponse == null) {
                log.debug("[콘텐츠 데이터 관리] SportResponse가 비어있습니다.");
                continue;
            }

            Long sportId = Long.valueOf(theSportResponse.idEvent());
            if(contentRepository.existsByApiId(sportId)) {
                log.debug("[콘텐츠 데이터 관리] sportResponse idEvent가 이미 존재합니다.");
                continue;
            }

            Content content = theSportsMapper.sportsResponseToContent(theSportResponse, "sport");
            Long apiId = Long.valueOf(theSportResponse.idEvent());
            content.setApiId(apiId);
            contentRepository.save(content);
        }
    }
}
