package com.codeit.playlist.domain.message.event.message;

import com.codeit.playlist.domain.message.dto.data.DirectMessageDto;
import java.util.UUID;

public record DirectMessageSentEvent(
    UUID conversationId,
    DirectMessageDto message
) {}
