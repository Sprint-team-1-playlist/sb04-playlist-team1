package com.codeit.playlist.domain.content.api.service;

import com.codeit.playlist.domain.content.api.handler.TheSportsDateHandler;
import com.codeit.playlist.domain.content.api.response.TheSportsResponse;
import com.codeit.playlist.domain.content.entity.Content;
import com.codeit.playlist.domain.content.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TheSportsApiService {
    private final TheSportsDateHandler theSportsDateHandler;
    private final ContentRepository contentRepository;

    @Transactional
    public void saveContentsUsingContents(LocalDate localDate) {
        List<TheSportsResponse> theSportsResponseList = theSportsDateHandler.getSportsEvent(localDate);
        for(int i = 0; i < theSportsResponseList.size(); i++) {
            TheSportsResponse theSportsResponse = theSportsResponseList.get(i);
            log.debug("TheSportsResponse 확인: event = {}, thumb = {}",
                    theSportsResponse.strEvent(), theSportsResponse.strThumb());
            Content content = Content.createSportsContent(
                    theSportsResponse.strEvent(),
                    theSportsResponse.strFilename(),
                    theSportsResponse.strThumb()
            );
            log.debug("매핑된 Content 확인: title = {}, thumbnailUrl = {}",
                    content.getTitle(), content.getThumbnailUrl());
            contentRepository.save(content); // 여기에서 저장함
        }
    }
}
