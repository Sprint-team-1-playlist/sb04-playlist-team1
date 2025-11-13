package com.codeit.playlist.domain.content.service.basic;

import com.codeit.playlist.domain.content.dto.data.ContentDto;
import com.codeit.playlist.domain.content.dto.request.ContentCreateRequest;
import com.codeit.playlist.domain.content.entity.Content;
import com.codeit.playlist.domain.content.entity.Tag;
import com.codeit.playlist.domain.content.entity.Type;
import com.codeit.playlist.domain.content.exception.ContentBadRequestException;
import com.codeit.playlist.domain.content.mapper.ContentMapper;
import com.codeit.playlist.domain.content.repository.ContentRepository;
import com.codeit.playlist.domain.content.service.ContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class BasicContentService implements ContentService {
    private final ContentRepository contentRepository;
    private final ContentMapper contentsMapper;

    @Transactional
    @Override
    public ContentDto create(ContentCreateRequest request, String thumbnail) {
        log.debug("컨텐츠 생성 시작 : request = {}", request);
        thumbnail = "testThumbnail.jpg";

        Tag tags = new Tag();
        log.debug("태그 생성 시작 : tags = {}", request.tags());
        for(int i=0; i<request.tags().size(); i++) {
            tags.setItems(request.tags().get(i));
        }
        log.info("태그 생성 완료 : tags = {}", tags);

        log.debug("타입 생성 시작 : type = {}", request.type());
        Type type = null;
        if(request.type() == null) {
            throw new ContentBadRequestException("타입을 입력해주세요.");
        }
        if(request.type().equals(Type.MOVIE.toString())) {
            type = Type.MOVIE;
        }
        if (request.type().equals(Type.SPORT.toString())) {
            type = Type.SPORT;
        }
        if (request.type().equals(Type.TV_SERIES.toString())) {
            type = Type.TV_SERIES;
        }

        log.info("타입 생성 완료 : type = {}", type);

        Content contents = new Content(
                type,
                request.title(),
                request.description(),
                thumbnail,
                tags.toString(),
                0,
                0,
                0);

        contentRepository.save(contents);
        log.info("컨텐츠 생성 완료, id = {}", contents.getId());
        return contentsMapper.toDto(contents);
    }
}
