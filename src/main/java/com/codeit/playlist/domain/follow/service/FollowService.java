package com.codeit.playlist.domain.follow.service;

import com.codeit.playlist.domain.follow.dto.data.FollowDto;
import com.codeit.playlist.domain.follow.dto.request.FollowRequest;

public interface FollowService {

  FollowDto create(FollowRequest followRequest);

}
