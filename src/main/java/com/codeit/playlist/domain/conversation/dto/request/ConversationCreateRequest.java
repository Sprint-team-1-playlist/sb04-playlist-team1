package com.codeit.playlist.domain.conversation.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ConversationCreateRequest(
    @NotNull
    UUID withUserId
) {
}
