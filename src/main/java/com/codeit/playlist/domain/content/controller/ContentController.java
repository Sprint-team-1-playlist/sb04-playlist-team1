package com.codeit.playlist.domain.content.controller;

import com.codeit.playlist.domain.content.dto.data.ContentDto;
import com.codeit.playlist.domain.content.dto.request.ContentCreateRequest;
import com.codeit.playlist.domain.content.dto.request.ContentCursorRequest;
import com.codeit.playlist.domain.content.dto.request.ContentUpdateRequest;
import com.codeit.playlist.domain.content.dto.response.CursorResponseContentDto;
import com.codeit.playlist.domain.content.service.basic.BasicContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/contents")
public class ContentController {
    private final BasicContentService contentService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ContentDto> create(@RequestPart ContentCreateRequest request,
                                             @RequestPart MultipartFile thumbnail) {
        ContentDto contents = contentService.create(request, thumbnail);
        return ResponseEntity.status(HttpStatus.CREATED).body(contents);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{contentId}")
    public ResponseEntity<ContentDto> update(@PathVariable UUID contentId,
                                             @RequestBody ContentUpdateRequest request,
                                             @RequestBody String thumbnail) {
        ContentDto updateContents = contentService.update(contentId, request, thumbnail);
        return ResponseEntity.ok(updateContents);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{contentId}")
    public ResponseEntity<Void> delete(@PathVariable UUID contentId) {
        contentService.delete(contentId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping
    public ResponseEntity<CursorResponseContentDto> get(@ModelAttribute ContentCursorRequest request) {
        CursorResponseContentDto response = contentService.get(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{contentId}")
    public ResponseEntity<ContentDto> searchById(@PathVariable UUID contentId) {
        ContentDto content = contentService.search(contentId);
        return ResponseEntity.ok(content);
    }
}
