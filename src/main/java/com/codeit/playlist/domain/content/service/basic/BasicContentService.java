package com.codeit.playlist.domain.content.service.basic;

import com.codeit.playlist.domain.content.dto.data.ContentDto;
import com.codeit.playlist.domain.content.dto.request.ContentCreateRequest;
import com.codeit.playlist.domain.content.dto.request.ContentUpdateRequest;
import com.codeit.playlist.domain.content.entity.Content;
import com.codeit.playlist.domain.content.entity.Tag;
import com.codeit.playlist.domain.content.entity.Type;
import com.codeit.playlist.domain.content.exception.ContentBadRequestException;
import com.codeit.playlist.domain.content.exception.ContentNotFoundException;
import com.codeit.playlist.domain.content.mapper.ContentMapper;
import com.codeit.playlist.domain.content.repository.ContentRepository;
import com.codeit.playlist.domain.content.repository.TagRepository;
import com.codeit.playlist.domain.content.service.ContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

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
        thumbnail = "testThumbnail.jpg"; // 더미데이터

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
        log.info("태그 생성 완료 : tags = {}", tagList);

        log.info("컨텐츠 생성 완료, id = {}, tag = {}", content.getId(), tagRepository.findByContentId(content.getId()));
        return contentMapper.toDto(content, tagList);
    }

    @Transactional
    @Override
    public ContentDto update(UUID contentId, ContentUpdateRequest request, String thumbnail) {
        log.debug("컨텐츠 수정 시작 : id = {}", contentId);
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> ContentNotFoundException.withId(contentId));

        thumbnail = "testThumbnail.jpg"; // 더미

        content.setTitle(request.title());
        content.setDescription(request.description());
        content.setThumbnailUrl(thumbnail);

        log.debug("태그 수정 시작 : tag = {}", request.tags());
        List<Tag> oldtags = tagRepository.findByContentId(contentId);
        if(!oldtags.isEmpty()) { // 비어있지 않다면, 싹 다 밀어버림
            tagRepository.deleteAll(oldtags);
        }

        List<String> newTags = request.tags();
        if(request.tags() == null) {
            newTags = List.of();
        }

        List<Tag> tagList = newTags.stream()
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .distinct()
                        .map(tagName -> new Tag(content, tagName))
                        .toList();

        tagRepository.saveAll(tagList);
        log.info("태그 수정 완료 : tag = {}", tagList);

        log.info("컨텐츠 수정 완료 : id = {}, tag = {}",
                content.getId(), tagRepository.findByContentId(content.getId()));
        return contentMapper.toDto(content, tagList);
    }

    @Transactional
    @Override
    public void delete(UUID contentId) {
        log.debug("컨텐츠 삭제 시작 : id = {}", contentId);
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> ContentNotFoundException.withId(contentId));

        List<Tag> tags = tagRepository.findByContentId(contentId);
        log.debug("태그 삭제 시작 : tag = {}", tags);
        tagRepository.deleteAll(tags); // contentId와 연결된 tags 리스트를 삭제함
        log.info("태그 삭제 완료 : tag = {}", tags);

        contentRepository.deleteById(contentId);
        log.info("컨텐츠 삭제 완료 : id = {}", contentId);
    }
}
