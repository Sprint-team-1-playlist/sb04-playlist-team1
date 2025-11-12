package com.codeit.playlist.domain.follow.dto.data;

import java.util.UUID;

public record FollowDto(
    UUID id,
    UUID followeeId,
    UUID followerId
) {

}
