package com.codeit.playlist.domain.content.service.basic;

import com.codeit.playlist.domain.content.dto.data.ContentDto;
import com.codeit.playlist.domain.content.dto.request.ContentCreateRequest;
import com.codeit.playlist.domain.content.dto.request.ContentCursorRequest;
import com.codeit.playlist.domain.content.dto.request.ContentUpdateRequest;
import com.codeit.playlist.domain.content.dto.response.CursorResponseContentDto;
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

import java.util.ArrayList;
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

        if(thumbnail == null || thumbnail.isBlank()) {
            throw new ContentBadRequestException("썸네일은 필수입니다.");
        }

        Content content = new Content(
                type,
                request.title(),
                request.description(),
                thumbnail,
                0,
                0,
                0);

        contentRepository.save(content);

        log.debug("태그 생성 시작 : tags = {}", request.tags());
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

        log.info("컨텐츠 생성 완료, cotnent = {}, tag = {}", content, tagList);
        return contentMapper.toDto(content, tagList);
    }

    @Transactional
    @Override
    public ContentDto update(UUID contentId, ContentUpdateRequest request, String thumbnail) {
        log.debug("컨텐츠 수정 시작 : id = {}", contentId);
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> ContentNotFoundException.withId(contentId));

        if(thumbnail != null && !thumbnail.isBlank()) {
            content.updateContent(request.title(), request.description(), thumbnail);
        } else {
            // 만약 썸네일이 업데이트 되지 않는다면, 제목과 설명만 업데이트하기
            content.updateContent(request.title(), request.description(), content.getThumbnailUrl());
        }

        List<Tag> oldtags = tagRepository.findByContentId(contentId);
        if(!oldtags.isEmpty()) { // 비어있지 않다면, 싹 다 밀어버림
            tagRepository.deleteAll(oldtags);
        }

        log.info("태그 수정 시작 : tag = {}", request.tags());

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
        if(contentRepository.existsById(contentId)) {
            log.debug("태그 삭제 시작 : tag = {}", tagRepository.findByContentId(contentId));
            tagRepository.deleteAllByContentId(contentId); // contentId와 연결된 tags 리스트를 삭제함
            log.info("태그 삭제 완료 : tag = {}", tagRepository.findByContentId(contentId));

            contentRepository.deleteById(contentId);
            log.info("컨텐츠 삭제 완료 : id = {}", contentId);
        } else {
            throw ContentNotFoundException.withId(contentId);
        }
    }

    @Transactional
    @Override
    public CursorResponseContentDto get(ContentCursorRequest request) {
        log.debug("커서 페이지네이션 컨텐츠 수집 시작, request = {}", request);
        int limit = request.limit();

        if(limit <= 0 || limit > 1000) {
            limit = 10;
        }

        log.info("요청 typeEqual : {}, keywordLike : {}, tagsIn : {}, cursor : {}, idAfter : {}, limit : {}, sortDirection : {}, sortBy : {}",
                request.typeEqual(), request.keywordLike(), request.tagsIn(), request.cursor(), request.idAfter(), request.limit(), request.sortDirection(), request.sortBy());

        String sortDirection = request.sortDirection().toString();
        log.info("sortDirection : {}", sortDirection);

        if(sortDirection == null) {
            sortDirection = "DESCENDING";
        }
        boolean ascending;

        switch(sortDirection) {
            case "ASCENDING":
                ascending = true;
                break;

            case "DESCENDING":
                ascending = false;
                break;

            default:
                throw new IllegalArgumentException("sortDirection was something wrong" + sortDirection);
        }
        log.info("after sortDirection : {}", sortDirection);

        List<Content> contents = contentRepository.searchContents(request, ascending);
        List<ContentDto> data = new ArrayList<>();

        String sortBy = request.sortBy();
        if(sortBy == null) {
            sortBy = "createdAt"; // 디폴트
        }
        log.info("sortBy : {}", sortBy);

        String cursor = request.cursor();
        String nextCursor = null;
        String nextIdAfter = request.idAfter();

        switch(sortBy) {
            case "createdAt" :
                nextCursor = contents.size() == limit + 1 ? contents.get(limit).getCreatedAt().toString() : null;
                nextIdAfter = contents.size() == limit + 1 ? contents.get(limit).getId().toString() : null;
                break;

            case "watcherCount" :
                nextCursor = contents.size() == limit + 1 ? String.valueOf(contents.get(limit).getWatcherCount()) : null;
                nextIdAfter = contents.size() == limit + 1 ? contents.get(limit).getId().toString() : null;
                break;

            case "rate" :
                nextCursor = contents.size() == limit + 1 ? String.valueOf(contents.get(limit).getAverageRating()) : null;
                nextIdAfter = contents.size() == limit + 1 ? contents.get(limit).getId().toString() : null;
                break;

            default:
                throw new IllegalArgumentException();
        }

        log.info("after sortBy : {}", sortBy);
        log.info("nextCursor : {}", nextCursor);
        log.info("nextIdAfter : {}", nextIdAfter);

        int size = contents.size();
        boolean hasNext = size == limit + 1;
        int totalCount = contents.size(); // 전체 페이지 데이터의 갯수
        log.info("totalCount : {}", totalCount);
        log.info("hasNext : {}", hasNext);

        int itemsToReturn = Math.min(contents.size(), limit);

        for(int i = 0; i < itemsToReturn; i++) {
            Content content = contents.get(i);
            data.add(contentMapper.toDto(content));
        }

        CursorResponseContentDto responseDto = new CursorResponseContentDto(data, nextCursor, nextIdAfter, hasNext, totalCount, sortBy, sortDirection);
        log.debug("커서 페이지네이션 컨텐츠 수집 완료, response = {}", responseDto);
        return responseDto;
    }
}
