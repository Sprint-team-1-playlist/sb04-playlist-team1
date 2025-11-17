package com.codeit.playlist.domain.user.dto.data;

import java.time.Instant;

public record GeneratedToken(
    String token,
    Instant expiresAt

) {

}
