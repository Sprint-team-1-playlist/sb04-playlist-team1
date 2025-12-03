package com.codeit.playlist.domain.watching.dto.response;

import java.util.UUID;

public record UserSummary(
        UUID userId,
        String name,
        String profileImageUrl
) {
}
