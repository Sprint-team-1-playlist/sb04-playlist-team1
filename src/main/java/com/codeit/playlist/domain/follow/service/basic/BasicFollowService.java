package com.codeit.playlist.domain.follow.service.basic;

import com.codeit.playlist.domain.follow.dto.data.FollowDto;
import com.codeit.playlist.domain.follow.dto.request.FollowRequest;
import com.codeit.playlist.domain.follow.entity.Follow;
import com.codeit.playlist.domain.follow.exception.FollowAlreadyExistsException;
import com.codeit.playlist.domain.follow.exception.FollowNotFoundException;
import com.codeit.playlist.domain.follow.exception.FollowSelfNotAllowedException;
import com.codeit.playlist.domain.follow.mapper.FollowMapper;
import com.codeit.playlist.domain.follow.repository.FollowRepository;
import com.codeit.playlist.domain.follow.service.FollowService;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.exception.UserNotFoundException;
import com.codeit.playlist.domain.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class BasicFollowService implements FollowService {

  private final FollowRepository followRepository;
  private final FollowMapper followMapper;
  private final UserRepository userRepository;

  @Override
  public FollowDto create(FollowRequest followRequest) {
    log.debug("[Follow] 팔로우 생성 시작: {}", followRequest);

//    UUID followerId = getCurrentUserId();

    UUID testFollowerId = UUID.fromString("11111111-1111-1111-1111-111111111111");

    if (testFollowerId.equals(followRequest.followeeId())) {
      throw FollowSelfNotAllowedException.withId(followRequest.followeeId());
    }

    User followee = userRepository.findById(followRequest.followeeId())
        .orElseThrow(() -> UserNotFoundException.withId(followRequest.followeeId()));

    boolean alreadyFollowing = followRepository.existsByFollowerIdAndFolloweeId(
        testFollowerId, followRequest.followeeId());
    if (alreadyFollowing) {
      throw FollowAlreadyExistsException.withId(followRequest.followeeId());
    }

    User follower = userRepository.findById(testFollowerId)
        .orElseThrow(() -> UserNotFoundException.withId(testFollowerId));

    Follow follow = new Follow(follower, followee);
    followRepository.save(follow);

    followee.increaseFollowCount();

    log.info("[Follow] 팔로우 생성 완료: {} -> {}", follower.getId(), followee.getId());
    FollowDto followDto = followMapper.toDto(follow);
    return followDto;
  }

  @Override
  public Boolean followedByMe(UUID followeeId) {
    log.debug("[Follow] 특정 유저를 내가 팔로우하는지 여부 조회 시작: {}", followeeId);
    //    UUID followerId = getCurrentUserId();
    UUID testFollowerId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    if (testFollowerId.equals(followeeId)) {
      throw FollowSelfNotAllowedException.withId(followeeId);
    }
    boolean isFollowing = followRepository.existsByFollowerIdAndFolloweeId(testFollowerId, followeeId);
    log.info("[Follow] 특정 유저를 내가 팔로우하는지 여부 조회 완료: {} -> {}", followeeId, isFollowing);
    return isFollowing;
  }

  @Override
  public Long countFollowers(UUID followeeId) {
    log.debug("[Follow] 특정 유저의 팔로워 수 조회 시작: {}", followeeId);
    User followee = userRepository.findById(followeeId)
        .orElseThrow(() -> UserNotFoundException.withId(followeeId));
    long followersCount = followee.getFollowCount();
    log.info("[Follow] 특정 유저의 팔로워 수 조회 완료: {}", followersCount);
    return followersCount;
  }

  @Override
  public void delete(UUID followId) {
    Follow follow = followRepository.findById(followId)
        .orElseThrow(() -> FollowNotFoundException.withId(followId));
    follow.getFollowee().decreaseFollowCount();
    followRepository.deleteById(followId);
  }

  private UUID getCurrentUserId() {
    //    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    //    PlaylistUserDetails userDetails = (PlaylistUserDetails) authentication.getPrincipal();
    //    return userDetails.getId();
    return null;
  }
}
