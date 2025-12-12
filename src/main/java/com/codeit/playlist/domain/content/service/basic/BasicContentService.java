package com.codeit.playlist.domain.content.service.basic;

import com.codeit.playlist.domain.content.dto.data.ContentDto;
import com.codeit.playlist.domain.content.dto.request.ContentCreateRequest;
import com.codeit.playlist.domain.content.dto.request.ContentCursorRequest;
import com.codeit.playlist.domain.content.dto.request.ContentUpdateRequest;
import com.codeit.playlist.domain.content.dto.response.CursorResponseContentDto;
import com.codeit.playlist.domain.content.entity.Content;
import com.codeit.playlist.domain.content.entity.Tag;
import com.codeit.playlist.domain.content.exception.ContentBadRequestException;
import com.codeit.playlist.domain.content.exception.ContentNotFoundException;
import com.codeit.playlist.domain.content.mapper.ContentMapper;
import com.codeit.playlist.domain.content.repository.ContentRepository;
import com.codeit.playlist.domain.content.repository.TagRepository;
import com.codeit.playlist.domain.content.service.ContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BasicContentService implements ContentService {
    private final ContentRepository contentRepository;
    private final TagRepository tagRepository;
    private final ContentMapper contentMapper;

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.content-bucket}")
    private String bucket;

    @Value("${cloud.aws.s3.directory}")
    private String directory;

    @Value("${cloud.aws.s3.base-url}")
    private String baseUrl;


    @Transactional
    @Override
    public ContentDto create(ContentCreateRequest request, MultipartFile thumbnail) {
        log.debug("[콘텐츠 데이터 관리] 관리자 권한 컨텐츠 생성 시작 : request = {}", request);

        log.debug("[콘텐츠 데이터 관리] 타입 생성 시작 : type = {}", request.type());

        if(request.type() == null) {
            throw new ContentBadRequestException("타입을 입력해주세요.");
        }

        if(thumbnail == null || thumbnail.isEmpty()) {
            throw new ContentBadRequestException("썸네일은 필수입니다.");
        }

        String strThumbnail = saveImageToS3(thumbnail);
        Long uuid = UUID.randomUUID().getLeastSignificantBits();

        Content content = new Content(
                uuid,
                request.type(),
                request.title(),
                request.description(),
                strThumbnail,
                0,
                0,
                0);

        contentRepository.save(content);

        log.debug("[콘텐츠 데이터 관리] 태그 생성 시작 : tags = {}", request.tags());
        List<Tag> tagList = request.tags().stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .map(tagName -> new Tag(content, tagName))
                .toList();

        if(!tagList.isEmpty()) {
            tagRepository.saveAll(tagList);
        }
        log.info("[콘텐츠 데이터 관리] 태그 생성 완료 : tags = {}", tagList);

        log.info("[콘텐츠 데이터 관리] 컨텐츠 생성 완료, cotnent = {}, tag = {}", content, tagList);
        return contentMapper.toDto(content, tagList);
    }

    @Transactional
    @Override
    public ContentDto update(UUID contentId, ContentUpdateRequest request, String thumbnail) {
        log.debug("[콘텐츠 데이터 관리] 컨텐츠 수정 시작 : id = {}", contentId);
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

        log.info("[콘텐츠 데이터 관리] 태그 수정 시작 : tag = {}", request.tags());

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
        log.info("[콘텐츠 데이터 관리] 태그 수정 완료 : tag = {}", tagList);

        log.info("[콘텐츠 데이터 관리] 컨텐츠 수정 완료 : id = {}, tag = {}",
                content.getId(), tagRepository.findByContentId(content.getId()));
        return contentMapper.toDto(content, tagList);
    }

    @Transactional
    @Override
    public void delete(UUID contentId) {
        log.debug("[콘텐츠 데이터 관리] 컨텐츠 삭제 시작 : id = {}", contentId);
        if(contentRepository.existsById(contentId)) {
            log.debug("[콘텐츠 데이터 관리] 태그 삭제 시작 : tag = {}", tagRepository.findByContentId(contentId));
            tagRepository.deleteAllByContentId(contentId); // contentId와 연결된 tags 리스트를 삭제함
            log.info("[콘텐츠 데이터 관리] 태그 삭제 완료 : tag = {}", tagRepository.findByContentId(contentId));

            contentRepository.deleteById(contentId);
            log.info("[콘텐츠 데이터 관리] 컨텐츠 삭제 완료 : id = {}", contentId);
        } else {
            throw ContentNotFoundException.withId(contentId);
        }
    }

    @Transactional
    @Override
    public CursorResponseContentDto get(ContentCursorRequest request) {
        log.debug("[콘텐츠 데이터 관리] 커서 페이지네이션 컨텐츠 수집 시작, request = {}", request);
        int limit = request.limit();

        if(limit <= 0 || limit > 1000) {
            limit = 10;
        }

        String sortDirection = request.sortDirection() != null ? request.sortDirection().toString() : "DESCENDING";
        boolean ascending;

        switch(sortDirection) {
            case "ASCENDING":
                ascending = true;
                break;

            case "DESCENDING":
                ascending = false;
                break;

            default:
                throw new IllegalArgumentException("[콘텐츠 데이터 관리] sortDirection was something wrong : " + sortDirection);
        }

        String sortBy = request.sortBy();
        if(sortBy == null) {
            sortBy = "createdAt"; // 디폴트
        }

        List<Content> contents = contentRepository.searchContents(request, ascending, limit, sortBy);
        // hasNext 판단용으로 limit + 1개를 가져왔으니 실제 반환할 데이터는 limit개까지만
        int actualLimitSize = Math.min(contents.size(), limit);
        List<ContentDto> data = new ArrayList<>();

        /**
         * N+1 쿼리 문제 및 반환 데이터 크기 오류, codeRabbit 참조
         * 192 - 205
         */
        List<UUID> contentIds = contents.stream()
                .limit(actualLimitSize)
                .map(Content::getId)
                .toList();

        List<Tag> allTags = tagRepository.findByContentIdIn(contentIds);
        Map<UUID, List<Tag>> tagsByContentId = allTags.stream()
                .collect(Collectors.groupingBy(tag -> tag.getContent().getId()));

        for(int i=0; i < actualLimitSize; i++) {
            Content content = contents.get(i);
            List<Tag> tags = tagsByContentId.getOrDefault(content.getId(), List.of());
            data.add(contentMapper.toDto(content, tags));
        }

        String nextCursor = null;
        String nextIdAfter = null;

        int size = contents.size();
        boolean hasNext = size == limit + 1;
        int pageSize = Math.min(size, limit);

        if(hasNext && pageSize > 0) {
            Content lastPage = contents.get(pageSize - 1); // 이번 페이지에서 실제로 반환되는 마지막 요소

            switch(sortBy) {
                case "createdAt":
                    nextCursor = lastPage.getCreatedAt().toString();
                    break;

                case "watcherCount":
                    nextCursor = String.valueOf(lastPage.getWatcherCount());
                    break;

                case "rate":
                    nextCursor = String.valueOf(lastPage.getAverageRating());
                    break;

                default:
                    throw new IllegalArgumentException("[콘텐츠 데이터 관리] sortBy was something wrong : " + sortBy);
            }

            nextIdAfter = lastPage.getId().toString();
        }

        CursorResponseContentDto responseDto = new CursorResponseContentDto(data, nextCursor, nextIdAfter, hasNext, pageSize, sortBy, sortDirection);
        log.debug("[콘텐츠 데이터 관리] 커서 페이지네이션 컨텐츠 수집 완료");
        return responseDto;
    }

    @Transactional
    public ContentDto search(UUID contentId) {
        log.info("[콘텐츠 데이터 관리] 컨텐츠 데이터 단건 조회 시작, contentId : {}",contentId);
        Content searchContent = contentRepository.findById(contentId)
                .orElseThrow(() -> ContentNotFoundException.withId(contentId));
        List<Tag> tags = tagRepository.findByContentId(searchContent.getId());
        log.info("[콘텐츠 데이터 관리] 컨텐츠 데이터 단건 조회 완료, searchContent : {}", searchContent);
        return contentMapper.toDto(searchContent,tags);
    }

    @Override
    public String saveImageToS3(MultipartFile file) {
        log.debug("[콘텐츠 데이터 관리] 썸네일 MultipartFile S3업로드 시작");

        if(file == null || file.isEmpty()) {
            log.debug("[콘텐츠 데이터 관리] file이 없어요.");
            throw new ContentBadRequestException("업로드 할 파일이 없어요.");
        }

        String contentType = file.getContentType();
        if(contentType == null || !contentType.startsWith("image/")) {
            throw new ContentBadRequestException("이미지 파일만 업로드 할 수 있어요.");
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if(originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String key = directory + UUID.randomUUID() + extension;

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );

            String url = baseUrl + key;
            log.info("[콘텐츠 데이터 관리] MultipartFile S3 업로드 완료, url : {}", url);
            return url;

        } catch(IOException e) {
            log.error("[콘텐츠 데이터 관리] 썸네일 MultipartFile S3업로드 실패",e);
            throw new ContentBadRequestException("파일 업로드 중 오류가 발생했어요.");
        }
    }
}
