package com.codeit.playlist.domain.watching.dto.response;


public record ContentChatDto(
        UserSummary sender,
        String content
) {
}
