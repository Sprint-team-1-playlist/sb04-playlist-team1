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
import com.codeit.playlist.domain.file.S3Uploader;
import com.codeit.playlist.domain.playlist.repository.PlaylistContentRepository;
import com.codeit.playlist.domain.playlist.repository.PlaylistRepository;
import com.codeit.playlist.domain.watching.repository.RedisWatchingSessionRepository;
import com.codeit.playlist.global.constant.S3Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BasicContentService implements ContentService {
    private final ContentRepository contentRepository;
    private final TagRepository tagRepository;
    private final ContentMapper contentMapper;
    private final S3Uploader s3Uploader;
    private final S3Properties s3Properties;
    private final RedisWatchingSessionRepository redisWatchingSessionRepository;
    private final PlaylistRepository playlistRepository;
    private final PlaylistContentRepository playlistContentRepository;

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

        String imageKey = saveImageToS3(thumbnail);
        s3Uploader.upload(s3Properties.getContentBucket(), imageKey, thumbnail);
        Long uuid = UUID.randomUUID().getLeastSignificantBits();

        Content content = new Content(
                uuid,
                request.type(),
                request.title(),
                request.description(),
                imageKey,
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

        log.info("[콘텐츠 데이터 관리] 컨텐츠 생성 완료, cotnent = {}, tagList = {}", content, tagList);

        ContentDto mapDto = contentMapper.toDtoUsingS3(content, tagList, s3Properties);
        return mapDto;
    }

    @Transactional
    @Override
    public ContentDto update(UUID contentId, ContentUpdateRequest request, MultipartFile thumbnail) {
        log.debug("[콘텐츠 데이터 관리] 컨텐츠 수정 시작 : id = {}", contentId);
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> ContentNotFoundException.withId(contentId));

        String currentImagekey = content.getThumbnailUrl(); // key
        String updateImageKey = currentImagekey;

        if(thumbnail != null && !thumbnail.isEmpty()) { // 만약 썸네일이 들어왔다면, 저장함
            String newImageKey = saveImageToS3(thumbnail); // 새로운 썸네일 key를 생성하고,
            s3Uploader.upload(s3Properties.getContentBucket(), newImageKey, thumbnail); // 업로드한다
            updateImageKey = newImageKey;// 이걸 업데이트 이미지에 넣어준다
        }

        List<Tag> oldtags = tagRepository.findByContentId(contentId);
        if(!oldtags.isEmpty()) { // 비어있지 않다면, 싹 다 밀어버림
            tagRepository.deleteAll(oldtags);
        }

        log.info("[콘텐츠 데이터 관리] 태그 수정 시작 : tag = {}", request.tags());

        List<String> updateTags = request.tags();
        if(request.tags() == null) {
            updateTags = List.of();
        }

        List<Tag> tagList = updateTags.stream()
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .distinct()
                        .map(tagName -> new Tag(content, tagName))
                        .toList();

        tagRepository.saveAll(tagList);
        log.info("[콘텐츠 데이터 관리] 태그 수정 완료 : tag = {}", tagList);

        content.updateContent(request.title(), request.description(), updateImageKey);
        if(!Objects.equals(updateImageKey, currentImagekey)) {
            deleteImageFromS3(currentImagekey); // 기존 이미지는 삭제한다
        }

        log.info("[콘텐츠 데이터 관리] 컨텐츠 수정 완료 : id = {}, tag = {}",
                content.getId(), tagRepository.findByContentId(content.getId()));

        ContentDto mapDto = contentMapper.toDtoUsingS3(content, tagList, s3Properties); // 맵핑을 통해 Dto로 변환
        return mapDto;
    }

    @Transactional
    @Override
    public void delete(UUID contentId) {
        log.debug("[콘텐츠 데이터 관리] 컨텐츠 삭제 시작 : id = {}", contentId);
        if(contentRepository.existsById(contentId)) {
            playlistContentRepository.deleteAllByContent_Id(contentId);
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
            content.setWatcherCount(redisWatchingSessionRepository.countWatchingSessionByContentId(content.getId()));
            List<Tag> tags = tagsByContentId.getOrDefault(content.getId(), List.of());
            ContentDto mapDto = contentMapper.toDtoUsingS3(content, tags, s3Properties);
            data.add(mapDto);
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
        ContentDto mapDto = contentMapper.toDtoUsingS3(searchContent, tags, s3Properties);
        return mapDto;
    }

    private String saveImageToS3(MultipartFile file) {
        log.debug("[콘텐츠 데이터 관리] 썸네일 MultipartFile S3업로드 시작");

        if(file == null || file.isEmpty()) {
            log.debug("[콘텐츠 데이터 관리] file이 없어요.");
            throw new ContentBadRequestException("업로드 할 파일이 없어요.");
        }

        String contentType = file.getContentType();
        if(contentType == null || !contentType.startsWith("image/")) {
            throw new ContentBadRequestException("이미지 파일만 업로드 할 수 있어요.");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if(originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String s3ImageKey = UUID.randomUUID() + extension;

        return s3ImageKey;
    }

    private void deleteImageFromS3(String key) {
        if(key != null && !key.isEmpty()) {
            s3Uploader.delete(s3Properties.getContentBucket(), key);
        }
    }

    private String s3Url(String s3ImageKey) {
        // https://(버킷명).s3.(지역명).amazonaws.com/(이미지 키)
        String url = "https://" + s3Properties.getContentBucket() + ".s3." + s3Properties.getRegion() + ".amazonaws.com/" + s3ImageKey;
        return url;
    }
}
