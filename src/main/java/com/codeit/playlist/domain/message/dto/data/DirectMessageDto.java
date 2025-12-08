package com.codeit.playlist.domain.message.dto.data;

import com.codeit.playlist.domain.user.dto.data.UserSummary;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record DirectMessageDto(
    @NotNull
    UUID id,

    @NotNull
    UUID conversationId,
    Instant createdAt,

    @NotNull
    UserSummary sender,

    @NotNull
    UserSummary receiver,

    @NotNull
    @Size(min = 1, max = 1000)
    String content
) {

}
