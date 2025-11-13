package com.codeit.playlist.domain.content.service.basic;

import com.codeit.playlist.domain.content.dto.data.ContentDto;
import com.codeit.playlist.domain.content.dto.request.ContentCreateRequest;
import com.codeit.playlist.domain.content.entity.Content;
import com.codeit.playlist.domain.content.entity.Tag;
import com.codeit.playlist.domain.content.entity.Type;
import com.codeit.playlist.domain.content.exception.ContentBadRequestException;
import com.codeit.playlist.domain.content.mapper.ContentMapper;
import com.codeit.playlist.domain.content.repository.ContentRepository;
import com.codeit.playlist.domain.content.repository.TagRepository;
import com.codeit.playlist.domain.content.service.ContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class BasicContentService implements ContentService {
    private final ContentRepository contentRepository;
    private final TagRepository tagRepository;
    private final ContentMapper contentMapper;

    @Transactional
    @Override
    public ContentDto create(ContentCreateRequest request, String thumbnail) {
        log.debug("컨텐츠 생성 시작 : request = {}", request);
        thumbnail = "testThumbnail.jpg";

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

        Content content = new Content(
                type,
                request.title(),
                request.description(),
                thumbnail,
                0,
                0,
                0);

        contentRepository.save(content);

        log.info("태그 생성 시작 : tags = {}", request.tags()); // 테스트 확인용 info
        List<Tag> tagList = request.tags().stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .map(tagName -> new Tag(content, tagName))
                .toList();

        if(!tagList.isEmpty()) {
            tagRepository.saveAll(tagList);
        }
        log.info("태그 생성 완료 : tags = {}", tagRepository.findAll());

        log.info("컨텐츠 생성 완료, id = {}, tag = {}", content.getId(), tagRepository.findByContentId(content.getId()));
        return contentMapper.toDto(content, tagList);
    }
}
