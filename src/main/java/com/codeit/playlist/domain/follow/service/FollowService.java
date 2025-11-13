package com.codeit.playlist.domain.follow.service;

import com.codeit.playlist.domain.follow.dto.data.FollowDto;
import com.codeit.playlist.domain.follow.dto.request.FollowRequest;
import java.util.UUID;

public interface FollowService {

  FollowDto create(FollowRequest followRequest);

  Boolean followedByMe(UUID followeeId);
}
