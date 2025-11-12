package com.codeit.playlist.domain.follow.service.basic;

import com.codeit.playlist.domain.follow.dto.data.FollowDto;
import com.codeit.playlist.domain.follow.dto.request.FollowRequest;
import com.codeit.playlist.domain.follow.entity.Follow;
import com.codeit.playlist.domain.follow.mapper.FollowMapper;
import com.codeit.playlist.domain.follow.repository.FollowRepository;
import com.codeit.playlist.domain.follow.service.FollowService;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.exception.UserNotFoundException;
import com.codeit.playlist.domain.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class BasicFollowService implements FollowService {

  private final FollowRepository followRepository;
  private final FollowMapper followMapper;
  private final UserRepository userRepository;

  @Override
  public FollowDto create(FollowRequest followRequest) {
    log.debug("팔로우 생성 시작: {}", followRequest);

    User followee = userRepository.findById(followRequest.followeeId())
        .orElseThrow(() -> UserNotFoundException.withId(followRequest.followeeId()));

//    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//    PlaylistUserDetails userDetails = (PlaylistUserDetails) authentication.getPrincipal();
//    User follower = userRepository.findById(userDetails.getId())
//        .orElseThrow(() -> UserNotFoundException.withId(userDetails.getId()));

    UUID testFollowerId = UUID.fromString("11111111-1111-1111-1111-111111111111");

    User follower = userRepository.findById(testFollowerId)
        .orElseThrow(() -> UserNotFoundException.withId(testFollowerId));

    Follow follow = new Follow(follower, followee);
    followRepository.save(follow);

    log.info("팔로우 생성 완료: {} -> {}", follower.getId(), followee.getId());
    FollowDto followDto = followMapper.toDto(follow);
    return followDto;
  }
}
