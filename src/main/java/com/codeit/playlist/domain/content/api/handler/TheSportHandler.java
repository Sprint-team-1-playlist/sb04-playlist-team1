package com.codeit.playlist.domain.content.api.handler;

import com.codeit.playlist.domain.content.api.mapper.TheSportMapper;
import com.codeit.playlist.domain.content.api.response.TheSportResponse;
import com.codeit.playlist.domain.content.api.service.TheSportApiService;
import com.codeit.playlist.domain.content.entity.Content;
import com.codeit.playlist.domain.content.repository.ContentRepository;
import com.codeit.playlist.domain.content.service.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class TheSportHandler {
    private final ContentRepository contentRepository;
    private final TheSportApiService theSportApiService;
    private final TheSportMapper theSportsMapper;
    private final TagService tagService;

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

            final Long sportId;
            try {
                sportId = Long.valueOf(theSportResponse.idEvent());
            } catch(RuntimeException e) {
                log.warn("[콘텐츠 데이터 관리] sportResponse idEvent 파싱 실패, idEvent={},",theSportResponse.idEvent(), e);
                continue;
            }

            if(contentRepository.existsByTypeAndApiId("sport", sportId)) {
                log.debug("[콘텐츠 데이터 관리] sportResponse idEvent가 이미 존재합니다.");
                continue;
            }

            Content content = theSportsMapper.sportsResponseToContent(theSportResponse, "sport");

            List<String> tags = Stream.of(
                    theSportResponse.strSport(),
                    theSportResponse.strHomeTeam(),
                    theSportResponse.strAwayTeam())
                    .filter(tag -> tag != null && !tag.isBlank())
                    .toList();

            contentRepository.save(content);
            if(!tags.isEmpty()) {
                tagService.saveTheSportTagToContent(content, tags);
            }
        }
    }
}
