package com.codeit.playlist.domain.message.controller;

import com.codeit.playlist.domain.message.dto.response.CursorResponseDirectMessageDto;
import com.codeit.playlist.domain.message.service.MessageService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
      @RequestParam String cursor,
      @RequestParam UUID idAfter,
      @RequestParam int limit,
      @RequestParam String sortDirection,
      @RequestParam String sortBy) {
    log.debug("[Message] findAll");

    CursorResponseDirectMessageDto cursorMessageDto = messageService.findAll(conversationId, cursor, idAfter, limit, sortDirection, sortBy);

    log.info("[Message] findAll");
    return ResponseEntity
        .status(HttpStatus.OK)
        .body(cursorMessageDto);
  }
}
