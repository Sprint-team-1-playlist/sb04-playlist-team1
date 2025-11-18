package com.codeit.playlist.domain.conversation.dto.data;

import com.codeit.playlist.domain.message.dto.data.DirectMessageDto;
import com.codeit.playlist.domain.user.dto.data.UserSummary;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ConversationDto(
    @NotNull
    UUID id,
    @NotNull
    UserSummary with,
    DirectMessageDto lastestMessage,
    boolean hasUnread
) {

}
