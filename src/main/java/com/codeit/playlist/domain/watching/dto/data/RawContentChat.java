package com.codeit.playlist.domain.watching.dto.data;

import java.util.UUID;

public record RawContentChat(
        UUID userId,
        String content
) {
}
