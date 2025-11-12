package com.codeit.playlist.domain.conversation.dto.data;

import com.codeit.playlist.domain.user.dto.data.UserSummary;
import java.util.UUID;

public record ConversationDto(
    UUID id,
    UserSummary with,
    DirectMessageDto lastestMessage,
    boolean hasUnread
) {

}
