package com.codeit.playlist.domain.content.controller;

import com.codeit.playlist.domain.content.dto.data.ContentDto;
import com.codeit.playlist.domain.content.dto.request.ContentCreateRequest;
import com.codeit.playlist.domain.content.dto.request.ContentUpdateRequest;
import com.codeit.playlist.domain.content.service.basic.BasicContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/contents")
public class ContentController {
    private final BasicContentService contentService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ContentDto> create(@RequestBody ContentCreateRequest request,
                                             @RequestBody String thumbnail) {
        ContentDto contents = contentService.create(request, thumbnail);
        return ResponseEntity.status(HttpStatus.OK).body(contents);
    }

    @PatchMapping("/{contentId}")
    public ResponseEntity<ContentDto> update(@PathVariable UUID contentId,
                                             @RequestBody ContentUpdateRequest request,
                                             @RequestBody String thumbnail) {
        ContentDto updateContents = contentService.update(contentId, request, thumbnail);
        return ResponseEntity.ok(updateContents);
    }
}
