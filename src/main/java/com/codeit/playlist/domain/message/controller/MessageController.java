package com.codeit.playlist.domain.message.controller;

import com.codeit.playlist.global.error.InvalidSortByException;
import com.codeit.playlist.global.error.InvalidSortDirectionException;
import com.codeit.playlist.domain.message.dto.response.CursorResponseDirectMessageDto;
import com.codeit.playlist.domain.message.service.MessageService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@Validated
@RequestMapping("/api/conversations")
public class MessageController {

  private final MessageService messageService;

  @GetMapping("/{conversationId}/direct-messages")
  public ResponseEntity<CursorResponseDirectMessageDto> findAll(@PathVariable UUID conversationId,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) UUID idAfter,
      @RequestParam @Min(1) @Max(100) int limit,
      @RequestParam String sortDirection,
      @RequestParam String sortBy) {
    if (!sortBy.equals("createdAt")) {
      throw InvalidSortByException.withSortBy(sortBy);
    }
    if (!sortDirection.equals("DESCENDING")) {
      throw InvalidSortDirectionException.withSortDirection(sortDirection);
    }

    log.debug("[Message] DM 목록 조회 요청: {}", conversationId);

    CursorResponseDirectMessageDto cursorMessageDto = messageService.findAll(conversationId, cursor, idAfter, limit, sortDirection, sortBy);

    log.info("[Message] DM 목록 조회 응답: {}", cursorMessageDto);
    return ResponseEntity
        .status(HttpStatus.OK)
        .body(cursorMessageDto);
  }

  @PostMapping("/{conversationId}/direct-messages/{directMessageId}/read")
  public ResponseEntity<Void> markAsRead(@PathVariable UUID conversationId,
      @PathVariable UUID directMessageId){
    log.debug("[Message] DM 읽음 처리 요청: {}, {}", conversationId, directMessageId);

    messageService.markMessageAsRead(conversationId, directMessageId);

    log.info("[Message] DM 읽음 처리 응답");
    return ResponseEntity
        .status(HttpStatus.OK)
        .build();
  }
}
