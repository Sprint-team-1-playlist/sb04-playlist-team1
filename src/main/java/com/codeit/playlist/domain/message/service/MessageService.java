package com.codeit.playlist.domain.message.service;

import com.codeit.playlist.domain.message.dto.data.DirectMessageDto;
import com.codeit.playlist.domain.message.dto.request.DirectMessageSendRequest;
import java.util.UUID;

public interface MessageService {
  DirectMessageDto save(UUID conversationId, DirectMessageSendRequest sendRequest);
}
