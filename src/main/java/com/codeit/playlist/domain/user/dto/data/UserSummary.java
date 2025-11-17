package com.codeit.playlist.domain.user.dto.data;

import java.util.UUID;

public record UserSummary(
    UUID userId,
    String name,
    String profileImageUrl
) {
}
