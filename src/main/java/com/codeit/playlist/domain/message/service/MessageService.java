package com.codeit.playlist.domain.message.service;

import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.message.dto.data.DirectMessageDto;
import com.codeit.playlist.domain.message.dto.data.MessageSortBy;
import com.codeit.playlist.domain.message.dto.request.DirectMessageSendRequest;
import com.codeit.playlist.domain.message.dto.response.CursorResponseDirectMessageDto;
import java.security.Principal;
import java.util.UUID;

public interface MessageService {
  DirectMessageDto save(UUID conversationId, DirectMessageSendRequest sendRequest, Principal principal);

  CursorResponseDirectMessageDto findAll(UUID conversationId,
      String cursor,
      UUID idAfter,
      int limit,
      SortDirection sortDirection,
      MessageSortBy sortBy,
      Principal principal);

  void markMessageAsRead(UUID conversationId, UUID directMessageId, Principal principal);
}
