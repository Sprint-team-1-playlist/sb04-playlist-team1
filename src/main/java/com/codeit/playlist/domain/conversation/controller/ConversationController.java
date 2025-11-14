package com.codeit.playlist.domain.conversation.controller;

import com.codeit.playlist.domain.conversation.dto.data.ConversationDto;
import com.codeit.playlist.domain.conversation.dto.request.ConversationCreateRequest;
import com.codeit.playlist.domain.conversation.service.basic.BasicConversationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

  private final BasicConversationService conversationService;

  @PostMapping
  public ResponseEntity<ConversationDto> create(@RequestBody ConversationCreateRequest request){
    log.debug("[Conversation] 대화 생성 요청: {}", request);
    ConversationDto conversationDto = conversationService.create(request);
    log.info("[Conversation] 대화 생성 응답: {}", conversationDto);
    return ResponseEntity
        .status(HttpStatus.OK)
        .body(conversationDto);
  }
}
