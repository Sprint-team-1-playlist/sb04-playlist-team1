package com.codeit.playlist.domain.message.dto.data;

import com.codeit.playlist.domain.user.dto.data.UserSummary;
import java.time.LocalDateTime;
import java.util.UUID;

public record DirectMessageDto(
    UUID id,
    UUID conversationId,
    LocalDateTime createdAt,
    UserSummary sender,
    UserSummary receiver,
    String content
) {

}
