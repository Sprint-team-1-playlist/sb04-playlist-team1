package com.codeit.playlist.domain.follow.dto.request;

import java.util.UUID;

public record FollowRequest(
    UUID followeeId
) {

}
