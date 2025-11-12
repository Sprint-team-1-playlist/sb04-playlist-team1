package com.codeit.playlist.domain.conversation.dto.request;

import java.util.UUID;

public record ConversationCreateRequest(
    UUID withUserId
) {

}
