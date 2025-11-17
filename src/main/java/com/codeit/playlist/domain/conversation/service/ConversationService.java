package com.codeit.playlist.domain.conversation.service;

import com.codeit.playlist.domain.conversation.dto.data.ConversationDto;
import com.codeit.playlist.domain.conversation.dto.request.ConversationCreateRequest;
import com.codeit.playlist.domain.conversation.dto.response.CursorResponseConversationDto;
import java.util.UUID;

public interface ConversationService {

  ConversationDto create(ConversationCreateRequest request);

  CursorResponseConversationDto findAll(String keywordLike,
      String cursor,
      UUID idAfter,
      int limit,
      String sortDirection,
      String sortBy);
}
