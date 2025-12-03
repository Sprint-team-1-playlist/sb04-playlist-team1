package com.codeit.playlist.domain.watching.dto.response;


import com.codeit.playlist.domain.user.dto.data.UserSummary;

public record ContentChatDto(
        UserSummary sender,
        String content
) {
}
