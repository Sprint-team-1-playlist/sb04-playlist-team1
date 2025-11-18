package com.codeit.playlist.domain.conversation.controller;

import com.codeit.playlist.domain.conversation.dto.data.ConversationDto;
import com.codeit.playlist.domain.conversation.dto.request.ConversationCreateRequest;
import com.codeit.playlist.domain.conversation.dto.response.CursorResponseConversationDto;
import com.codeit.playlist.domain.conversation.exception.conversation.InvalidSortByException;
import com.codeit.playlist.domain.conversation.exception.conversation.InvalidSortDirectionException;
import com.codeit.playlist.domain.conversation.service.ConversationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@Validated
@RequestMapping("/api/conversations")
public class ConversationController {

  private final ConversationService conversationService;

  @PostMapping
  public ResponseEntity<ConversationDto> create(@RequestBody ConversationCreateRequest request){
    log.debug("[Conversation] 대화 생성 요청: {}", request);
    ConversationDto conversationDto = conversationService.create(request);
    log.info("[Conversation] 대화 생성 응답: {}", conversationDto);
    return ResponseEntity
        .status(HttpStatus.OK)
        .body(conversationDto);
  }

  @GetMapping
  public ResponseEntity<CursorResponseConversationDto> findAll(@RequestParam(required = false) String keywordLike,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) UUID idAfter,
      @RequestParam @Min(1) @Max(100) int limit,
      @RequestParam(defaultValue = "ASCENDING") String sortDirection,
      @RequestParam(defaultValue = "createdAt") String sortBy
  ){
    if (!sortBy.equals("createdAt")) {
      throw InvalidSortByException.withSortBy(sortBy);
    }
    if (!sortDirection.equals("ASCENDING") && !sortDirection.equals("DESCENDING")) {
      throw InvalidSortDirectionException.withSortDirection(sortDirection);
    }
    log.debug("[Conversation] 대화 조회 시작");
    CursorResponseConversationDto cursorConversationDto = conversationService.findAll(keywordLike,
        cursor,
        idAfter,
        limit,
        sortDirection,
        sortBy);
    log.info("[Conversation] 대화 조회 응답: {}", cursorConversationDto);
    return  ResponseEntity
        .status(HttpStatus.OK)
        .body(cursorConversationDto);
  }
}
